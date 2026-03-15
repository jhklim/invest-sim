package com.jhklim.investsim.application.service;

import com.jhklim.investsim.application.dto.CreateStrategyCommand;
import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.application.port.in.StrategyUseCase;
import com.jhklim.investsim.application.port.out.*;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import com.jhklim.investsim.domain.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StrategyService implements StrategyUseCase {

    private final StrategyPort strategyPort;
    private final MemberPort memberPort;
    private final CurrentPricePort currentPricePort;
    private final ActiveStrategyPort activeStrategyPort;
    private final ActiveTradePort activeTradePort;
    private final TradePort tradePort;

    @Value("${app.strategy.max-per-member}")
    private int maxStrategiesPerMember;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initCache() {
        // 1. isActive=true 전략 (매수 대기 중)
        strategyPort.findAllActive().forEach(strategy -> {
            strategy.getBuyStrategies().size();
            strategy.getSellStrategies().size();
            activeStrategyPort.add(strategy);
        });

        // 2. OPEN 포지션이 있는 전략 (매도 대기 중, isActive=false 상태)
        tradePort.findAllByPositionStatus(PositionStatus.OPEN).forEach(trade -> {
            Strategy strategy = trade.getStrategy();
            strategy.getBuyStrategies().size();
            strategy.getSellStrategies().size();
            activeStrategyPort.add(strategy);
            activeTradePort.put(strategy.getId(), trade);
        });
    }

    public List<Strategy> findByMember(Long memberId) {
        return strategyPort.findByMemberId(memberId);
    }

    @Transactional
    public void create(Long memberId, CreateStrategyCommand command) {
        if (strategyPort.countByMemberId(memberId) >= maxStrategiesPerMember) {
            throw new BusinessException(ErrorCode.STRATEGY_LIMIT_EXCEEDED);
        }

        Member member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Strategy strategy = new Strategy(
                member,
                command.getName(),
                command.getDescription(),
                command.getExchange(),
                command.getMarket(),
                command.getBuyAmount()
        );

        command.getBuyConditions().forEach(c ->
                strategy.getBuyStrategies().add(new BuyStrategy(strategy, c.getIndicator(), c.getIndicatorValue())));

        command.getSellConditions().forEach(c ->
                strategy.getSellStrategies().add(new SellStrategy(strategy, c.getIndicator(), c.getIndicatorValue())));

        strategyPort.save(strategy);
    }

    // 전략 활성화 - 잔고 차감(묶기)
    @Transactional
    public void activate(Long memberId, Long strategyId) {
        Strategy strategy = strategyPort.findById(strategyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));

        if (!strategy.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 전략에 대한 권한이 없습니다.");
        }

        Member member = strategy.getMember();
        member.deductBalance(strategy.getBuyAmount());
        strategy.activate();
        strategy.getBuyStrategies().size();
        strategy.getSellStrategies().size();
        activeStrategyPort.add(strategy);
    }

    @Transactional
    public void deactivate(Long memberId, Long strategyId) {
        Strategy strategy = strategyPort.findById(strategyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));

        if (!strategy.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 전략에 대한 권한이 없습니다.");
        }

        // DB에서 현재 OPEN 포지션 조회
        tradePort.findByStrategyIdAndPositionStatus(strategyId, PositionStatus.OPEN)
                .ifPresentOrElse(
                        trade -> {
                            // OPEN 포지션 있음 → 현재가로 강제 청산 후 반환
                            BigDecimal currentPrice = currentPricePort.get(strategy.getMarket());
                            strategy.getMember().addBalance(trade.getOpenQuantity().multiply(currentPrice));
                            trade.close(currentPrice);
                            activeTradePort.remove(strategyId);
                        },
                        () -> {
                            if (strategy.isActive()) {
                                // 매수 대기 중 비활성화 → 예약금 반환
                                strategy.getMember().addBalance(strategy.getBuyAmount());
                            }
                            // isActive=false + OPEN 없음 = 이미 완료된 거래 → 잔고 변화 없음
                        }
                );

        strategy.deactivate();
        activeStrategyPort.remove(strategyId);
    }

    @Transactional
    public void delete(Long memberId, Long strategyId) {
        Strategy strategy = strategyPort.findById(strategyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));

        if (!strategy.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 전략에 대한 권한이 없습니다.");
        }

        // deactivate와 동일한 잔고/캐시 정리
        tradePort.findByStrategyIdAndPositionStatus(strategyId, PositionStatus.OPEN)
                .ifPresentOrElse(
                        trade -> {
                            BigDecimal currentPrice = currentPricePort.get(strategy.getMarket());
                            strategy.getMember().addBalance(trade.getOpenQuantity().multiply(currentPrice));
                            trade.close(currentPrice);
                            activeTradePort.remove(strategyId);
                        },
                        () -> {
                            if (strategy.isActive()) {
                                strategy.getMember().addBalance(strategy.getBuyAmount());
                            }
                        }
                );

        strategy.deactivate();
        activeStrategyPort.remove(strategyId);
        strategy.delete();
    }

    // buy() 체결 후 — isActive=false (사용자 제어 종료), 캐시는 유지 (매도 감시 계속)
    @Transactional
    public void deactivateAfterBuy(Long strategyId) {
        Strategy strategy = strategyPort.findById(strategyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
        strategy.deactivate();
    }

    // sell() 체결 후 — 캐시에서 제거 (이미 isActive=false)
    @Transactional
    public void autoDeactivateAfterSell(Long strategyId) {
        activeStrategyPort.remove(strategyId);
        activeTradePort.remove(strategyId);
    }
}