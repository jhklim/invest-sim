package com.jhklim.investsim.adapter.in.websocket;

import com.jhklim.investsim.adapter.out.upbit.CandleStore;
import com.jhklim.investsim.adapter.out.upbit.CurrentPriceStore;
import com.jhklim.investsim.adapter.out.upbit.dto.TradeTickData;
import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.application.dto.TradeOrderRequest;
import com.jhklim.investsim.application.port.in.StrategyUseCase;
import com.jhklim.investsim.application.port.in.TradeUseCase;
import com.jhklim.investsim.domain.model.CandleData;
import com.jhklim.investsim.domain.model.Exchange;
import com.jhklim.investsim.domain.model.Strategy;
import com.jhklim.investsim.domain.model.TradeSignal;
import com.jhklim.investsim.domain.service.StrategyEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * WebSocket으로 수신한 틱 데이터를 처리
 * - 현재가 업데이트
 * - 활성 전략 조회 및 신호 평가
 * - 매수/매도 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TickProcessor {

    private final CurrentPriceStore currentPriceStore;
    private final CandleStore candleStore;
    private final StrategyUseCase strategyUseCase;
    private final TradeUseCase tradeUseCase;
    private final StrategyEvaluator strategyEvaluator;

    public void process(TradeTickData tick) {
        currentPriceStore.update(tick.getMarket(), tick.getTradePrice());
        List<CandleData> candles = candleStore.get(tick.getMarket());

        ExchangeMarketSearchCond condition = new ExchangeMarketSearchCond(Exchange.UPBIT, tick.getMarket());
        List<Strategy> activeStrategies = strategyUseCase.findActiveStrategiesByMarket(condition);

        for (Strategy strategy : activeStrategies) {
            TradeSignal signal = strategyEvaluator.evaluate(strategy, candles);
            log.info("[{}] 전략: {} / 신호: {}", tick.getMarket(), strategy.getName(), signal);

            TradeOrderRequest order = new TradeOrderRequest(
                    tick.getTradePrice(),
                    calculateQuantity(strategy, tick.getTradePrice())
            );

            if (signal == TradeSignal.BUY) {
                try {
                    tradeUseCase.buy(strategy, order);
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("[BUY] 낙관적 락 충돌 - 전략: {}", strategy.getName());
                }
            } else if (signal == TradeSignal.SELL) {
                try {
                    tradeUseCase.sell(strategy, tick.getTradePrice());
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("[SELL] 낙관적 락 충돌 - 전략: {}", strategy.getName());
                }
            }
        }
    }

    private BigDecimal calculateQuantity(Strategy strategy, BigDecimal tradePrice) {
        return strategy.getBuyAmount().divide(tradePrice, 8, RoundingMode.HALF_UP);
    }
}