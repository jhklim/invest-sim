package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.PositionStatus;
import com.jhklim.investsim.domain.Trade;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
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
    private final CurrentPriceStore currentPriceStore;

    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        return strategyRepository.findActiveStrategiesByMarket(condition);
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
            // 매수 체결 전 -> buyAmount 그대로 환불
            strategy.getMember().addBalance(strategy.getBuyAmount());
        } else {
            // 매수 체결 후 -> 현재 가치로 반환
            BigDecimal currentPrice = currentPriceStore.get(strategy.getMarket());
            BigDecimal currentTotalValue = trade.getOpenQuantity().multiply(currentPrice);
            strategy.getMember().addBalance(currentTotalValue);
            trade.close(currentPrice);
        }

        strategy.deactivate();
    }
}