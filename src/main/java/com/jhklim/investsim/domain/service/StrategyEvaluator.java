package com.jhklim.investsim.domain.service;

import com.jhklim.investsim.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyEvaluator {

    private final IndicatorCalculator indicatorCalculator;

    public TradeSignal evaluate(Strategy strategy, List<CandleData> candles, Trade openTrade) {
        if (openTrade == null) {
            // 포지션 없음 → 매수 조건 체크
            boolean buySignal = checkAllBuyConditions(strategy.getBuyStrategies(), candles);
            return buySignal ? TradeSignal.BUY : TradeSignal.HOLD;
        }

        // 포지션 있음 → 매도 조건 체크
        boolean sellSignal = checkAllSellConditions(strategy.getSellStrategies(), candles);
        return sellSignal ? TradeSignal.SELL : TradeSignal.HOLD;
    }

    private boolean checkAllBuyConditions(List<BuyStrategy> buyStrategies, List<CandleData> candles) {
        if (buyStrategies.isEmpty()) return false;
        return buyStrategies.stream()
                .allMatch(buyStrategy
                        -> checkBuyCondition(buyStrategy.getIndicator(), buyStrategy.getIndicatorValue(), candles));
    }

    private boolean checkAllSellConditions(List<SellStrategy> conditions, List<CandleData> candles) {
        if (conditions.isEmpty()) return false;
        return conditions.stream()
                .allMatch(sellStrategy
                        -> checkSellCondition(sellStrategy.getIndicator(), sellStrategy.getIndicatorValue(), candles));
    }

    private boolean checkBuyCondition(Indicator indicator, double value, List<CandleData> candles) {
        BigDecimal threshold = BigDecimal.valueOf(value);
        return switch (indicator) {
            case RSI -> indicatorCalculator.calculateRsi(candles)
                    .map(rsi -> rsi.compareTo(threshold) <= 0)
                    .orElse(false);
            case VOLUME -> candles.get(candles.size() - 1).getVolume().compareTo(threshold) >= 0;
        };
    }

    private boolean checkSellCondition(Indicator indicator, double value, List<CandleData> candles) {
        BigDecimal threshold = BigDecimal.valueOf(value);
        return switch (indicator) {
            case RSI -> indicatorCalculator.calculateRsi(candles)
                    .map(rsi -> rsi.compareTo(threshold) >= 0)
                    .orElse(false);
            case VOLUME -> candles.get(candles.size() - 1).getVolume().compareTo(threshold) >= 0;
        };
    }
}