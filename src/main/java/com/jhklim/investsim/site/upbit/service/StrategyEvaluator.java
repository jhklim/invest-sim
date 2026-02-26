package com.jhklim.investsim.site.upbit.service;

import ch.qos.logback.core.util.StringUtil;
import com.jhklim.investsim.domain.PositionStatus;
import com.jhklim.investsim.domain.Trade;
import com.jhklim.investsim.domain.strategy.BuyStrategy;
import com.jhklim.investsim.domain.strategy.Indicator;
import com.jhklim.investsim.domain.strategy.SellStrategy;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.site.upbit.TradeSignal;
import com.jhklim.investsim.site.upbit.dto.CandleData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyEvaluator {

    private final IndicatorCalculator indicatorCalculator;

    public TradeSignal evaluate(Strategy strategy, List<CandleData> candles) {
        Trade activeTrade = strategy.getTrade();

        // 포지션 없으면 매수 조건만 체크
        if (activeTrade == null || activeTrade.getPositionStatus().equals(PositionStatus.CLOSE)) {
            boolean buySignal = checkAllBuyConditions(strategy.getBuyStrategies(), candles);
            return buySignal ? TradeSignal.BUY : TradeSignal.HOLD;
        }

        // 포지션 있으면 매도 조건만 체크
        if (activeTrade.getPositionStatus() == PositionStatus.OPEN) {
            boolean sellSignal = checkAllSellConditions(strategy.getSellStrategies(), candles);
            return sellSignal ? TradeSignal.SELL : TradeSignal.HOLD;
        }

        return TradeSignal.HOLD;

    }

    private boolean checkAllBuyConditions(List<BuyStrategy> buyStrategies, List<CandleData> candles) {
        if (buyStrategies.isEmpty()) return false;
        return buyStrategies.stream()
                .allMatch(buyStrategy 
                        -> checkBuyCondition(buyStrategy.getIndicator(), buyStrategy.getValue(), candles));
    }

    private boolean checkAllSellConditions(List<SellStrategy> conditions, List<CandleData> candles) {
        if (conditions.isEmpty()) return false;
        return conditions.stream()
                .allMatch(sellStrategy
                        -> checkSellCondition(sellStrategy.getIndicator(), sellStrategy.getValue(), candles));
    }

    private boolean checkBuyCondition(Indicator indicator, double value, List<CandleData> candles) {
        return switch (indicator) {
            case RSI -> indicatorCalculator.calculateRsi(candles) <= value;
            case VOLUME -> candles.get(candles.size() - 1).getVolume() >= value;
        };
    }

    private boolean checkSellCondition(Indicator indicator, double value, List<CandleData> candles) {
        return switch (indicator) {
            case RSI -> indicatorCalculator.calculateRsi(candles) >= value;
            case VOLUME -> candles.get(candles.size() - 1).getVolume() >= value;
        };
    }

}
