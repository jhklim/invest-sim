package com.jhklim.investsim.adapter.in.websocket;

import com.jhklim.investsim.adapter.out.upbit.CandleStore;
import com.jhklim.investsim.adapter.out.upbit.CurrentPriceStore;
import com.jhklim.investsim.adapter.out.upbit.dto.TradeTickData;
import com.jhklim.investsim.application.dto.TradeOrderRequest;
import com.jhklim.investsim.application.port.in.StrategyUseCase;
import com.jhklim.investsim.application.port.in.TradeUseCase;
import com.jhklim.investsim.application.port.out.ActiveStrategyPort;
import com.jhklim.investsim.application.port.out.ActiveTradePort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import com.jhklim.investsim.domain.model.CandleData;
import com.jhklim.investsim.domain.model.Exchange;
import com.jhklim.investsim.domain.model.Strategy;
import com.jhklim.investsim.domain.model.Trade;
import com.jhklim.investsim.domain.model.TradeSignal;
import com.jhklim.investsim.domain.service.StrategyEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ActiveStrategyPort activeStrategyPort;
    private final ActiveTradePort activeTradePort;
    private final TradeUseCase tradeUseCase;
    private final StrategyUseCase strategyUseCase;
    private final StrategyEvaluator strategyEvaluator;

    @Transactional
    public void process(TradeTickData tick) {
        currentPriceStore.update(tick.getMarket(), tick.getTradePrice());
        List<CandleData> candles = candleStore.get(tick.getMarket());

        List<Strategy> activeStrategies = activeStrategyPort.findByMarket(Exchange.UPBIT, tick.getMarket());

        for (Strategy strategy : activeStrategies) {
            Trade openTrade = activeTradePort.get(strategy.getId()).orElse(null);
            TradeSignal signal = strategyEvaluator.evaluate(strategy, candles, openTrade);
            log.info("[{}] 전략: {} / 신호: {}", tick.getMarket(), strategy.getName(), signal);

            if (signal == TradeSignal.BUY) {
                TradeOrderRequest order = new TradeOrderRequest(
                        tick.getTradePrice(),
                        calculateQuantity(strategy, tick.getTradePrice())
                );
                try {
                    Trade newTrade = tradeUseCase.buy(strategy, order);
                    activeTradePort.put(strategy.getId(), newTrade);
                    strategyUseCase.deactivateAfterBuy(strategy.getId());
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("[BUY] 낙관적 락 충돌 - 전략: {}", strategy.getName());
                }
            } else if (signal == TradeSignal.SELL) {
                try {
                    tradeUseCase.sell(strategy, tick.getTradePrice());
                    strategyUseCase.autoDeactivateAfterSell(strategy.getId());
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("[SELL] 낙관적 락 충돌 - 전략: {}", strategy.getName());
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.NO_OPEN_POSITION) {
                        log.warn("[SELL] 캐시 불일치 감지 - DB에 오픈 포지션 없음 - 전략: {}", strategy.getName());
                        strategyUseCase.autoDeactivateAfterSell(strategy.getId());
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private BigDecimal calculateQuantity(Strategy strategy, BigDecimal tradePrice) {
        return strategy.getBuyAmount().divide(tradePrice, 8, RoundingMode.HALF_UP);
    }
}