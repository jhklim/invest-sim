package com.jhklim.investsim.service;

import com.jhklim.investsim.controller.dto.CreateStrategyRequest;
import com.jhklim.investsim.controller.dto.StrategyResponse;
import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.PositionStatus;
import com.jhklim.investsim.domain.Trade;
import com.jhklim.investsim.domain.strategy.BuyStrategy;
import com.jhklim.investsim.domain.strategy.SellStrategy;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import com.jhklim.investsim.site.upbit.service.CurrentPriceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final MemberRepository memberRepository;
    private final CurrentPriceStore currentPriceStore;

    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        return strategyRepository.findActiveStrategiesByMarket(condition);
    }

    public List<StrategyResponse> findByMember(Long memberId) {
        return strategyRepository.findByMemberId(memberId).stream()
                .map(StrategyResponse::from)
                .toList();
    }

    @Transactional
    public void create(Long memberId, CreateStrategyRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Strategy strategy = new Strategy(
                member,
                request.getName(),
                request.getDescription(),
                request.getExchange(),
                request.getMarket(),
                request.getBuyAmount()
        );

        request.getBuyConditions().forEach(c ->
                strategy.getBuyStrategies().add(new BuyStrategy(strategy, c.getIndicator(), c.getIndicatorValue())));

        request.getSellConditions().forEach(c ->
                strategy.getSellStrategies().add(new SellStrategy(strategy, c.getIndicator(), c.getIndicatorValue())));

        strategyRepository.save(strategy);
    }

    // 전략 활성화 - 잔고 차감(묶기)
    @Transactional
    public void activate(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("전략이 존재하지 않습니다."));

        Member member = strategy.getMember();
        member.deductBalance(strategy.getBuyAmount());
        strategy.activate();
    }

    @Transactional
    public void deactivate(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("전략이 존재하지 않습니다."));

        Trade trade = strategy.getTrade();

        if (trade == null || trade.getPositionStatus() == PositionStatus.CLOSE) {
            strategy.getMember().addBalance(strategy.getBuyAmount());
        } else {
            BigDecimal currentPrice = currentPriceStore.get(strategy.getMarket());
            BigDecimal currentTotalValue = trade.getOpenQuantity().multiply(currentPrice);
            strategy.getMember().addBalance(currentTotalValue);
            trade.close(currentPrice);
        }

        strategy.deactivate();
    }
}