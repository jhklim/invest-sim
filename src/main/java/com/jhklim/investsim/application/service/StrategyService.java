package com.jhklim.investsim.application.service;

import com.jhklim.investsim.application.dto.CreateStrategyCommand;
import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.application.port.in.StrategyUseCase;
import com.jhklim.investsim.application.port.out.CurrentPricePort;
import com.jhklim.investsim.application.port.out.MemberPort;
import com.jhklim.investsim.application.port.out.StrategyPort;
import com.jhklim.investsim.domain.model.*;
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

    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        return strategyPort.findActiveStrategiesByMarket(condition);
    }

    public List<Strategy> findByMember(Long memberId) {
        return strategyPort.findByMemberId(memberId);
    }

    @Transactional
    public void create(Long memberId, CreateStrategyCommand command) {
        Member member = memberPort.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

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
                .orElseThrow(() -> new IllegalArgumentException("전략이 존재하지 않습니다."));

        if (!strategy.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 전략에 대한 권한이 없습니다.");
        }

        Member member = strategy.getMember();
        member.deductBalance(strategy.getBuyAmount());
        strategy.activate();
    }

    @Transactional
    public void deactivate(Long memberId, Long strategyId) {
        Strategy strategy = strategyPort.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("전략이 존재하지 않습니다."));

        if (!strategy.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 전략에 대한 권한이 없습니다.");
        }

        Trade trade = strategy.getTrade();

        if (trade == null || trade.getPositionStatus() == PositionStatus.CLOSE) {
            strategy.getMember().addBalance(strategy.getBuyAmount());
        } else {
            BigDecimal currentPrice = currentPricePort.get(strategy.getMarket());
            BigDecimal currentTotalValue = trade.getOpenQuantity().multiply(currentPrice);
            strategy.getMember().addBalance(currentTotalValue);
            trade.close(currentPrice);
        }

        strategy.deactivate();
    }
}