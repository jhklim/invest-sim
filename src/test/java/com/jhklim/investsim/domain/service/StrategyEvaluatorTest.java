package com.jhklim.investsim.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.application.dto.TradeOrderRequest;
import com.jhklim.investsim.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyEvaluatorTest {

    private StrategyEvaluator evaluator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        IndicatorCalculator calculator = new IndicatorCalculator();
        evaluator = new StrategyEvaluator(calculator);
        objectMapper = new ObjectMapper();
    }

    private CandleData candle(double closePrice, double volume) {
        try {
            String json = String.format(Locale.US,
                    "{\"trade_price\": %.2f, \"candle_acc_trade_volume\": %.2f}",
                    closePrice, volume);
            return objectMapper.readValue(json, CandleData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 포지션 없는 전략 (VOLUME 매수 조건)
     * ReflectionTestUtils: Spring이 제공하는 테스트 유틸 — private 필드에 값을 주입할 수 있음
     * JPA가 없는 순수 단위 테스트에서 엔티티 내부 상태를 설정할 때 사용
     */
    private Strategy strategyWithVolumeBuyCondition(double volumeThreshold) {
        Strategy strategy = new Strategy(null, Exchange.UPBIT, "KRW-BTC", BigDecimal.valueOf(100000));
        BuyStrategy buyCondition = new BuyStrategy(strategy, Indicator.VOLUME, volumeThreshold);
        ReflectionTestUtils.setField(strategy, "buyStrategies", List.of(buyCondition));
        ReflectionTestUtils.setField(strategy, "sellStrategies", List.of());
        // trade = null → 포지션 없음
        return strategy;
    }

    /**
     * OPEN 포지션이 있는 전략 (VOLUME 매도 조건)
     */
    private Strategy strategyWithVolumeSellConditionAndOpenTrade(double volumeThreshold) {
        Strategy strategy = new Strategy(null, Exchange.UPBIT, "KRW-BTC", BigDecimal.valueOf(100000));
        SellStrategy sellCondition = new SellStrategy(strategy, Indicator.VOLUME, volumeThreshold);
        ReflectionTestUtils.setField(strategy, "buyStrategies", List.of());
        ReflectionTestUtils.setField(strategy, "sellStrategies", List.of(sellCondition));

        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", BigDecimal.valueOf(1000000));
        TradeOrderRequest order = new TradeOrderRequest(BigDecimal.valueOf(50000), BigDecimal.valueOf(0.001));
        Trade openTrade = new Trade(member, strategy, order);  // 생성자에서 PositionStatus.OPEN 설정됨
        ReflectionTestUtils.setField(strategy, "trade", openTrade);

        return strategy;
    }

    // ──────────────── 매수 신호 ────────────────

    @Test
    @DisplayName("포지션 없고 VOLUME 조건 충족 → BUY")
    void evaluate_returnsBuy_whenNoTradeAndVolumeConditionMet() {
        Strategy strategy = strategyWithVolumeBuyCondition(50.0);
        List<CandleData> candles = List.of(candle(1000, 100.0));  // volume 100 >= threshold 50

        TradeSignal signal = evaluator.evaluate(strategy, candles);

        assertThat(signal).isEqualTo(TradeSignal.BUY);
    }

    @Test
    @DisplayName("포지션 없고 VOLUME 조건 미충족 → HOLD")
    void evaluate_returnsHold_whenNoTradeAndVolumeConditionNotMet() {
        Strategy strategy = strategyWithVolumeBuyCondition(200.0);
        List<CandleData> candles = List.of(candle(1000, 100.0));  // volume 100 < threshold 200

        TradeSignal signal = evaluator.evaluate(strategy, candles);

        assertThat(signal).isEqualTo(TradeSignal.HOLD);
    }

    @Test
    @DisplayName("매수 조건 목록이 비어있으면 → HOLD")
    void evaluate_returnsHold_whenBuyConditionsEmpty() {
        Strategy strategy = new Strategy(null, Exchange.UPBIT, "KRW-BTC", BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(strategy, "buyStrategies", List.of());
        ReflectionTestUtils.setField(strategy, "sellStrategies", List.of());
        List<CandleData> candles = List.of(candle(1000, 100.0));

        TradeSignal signal = evaluator.evaluate(strategy, candles);

        assertThat(signal).isEqualTo(TradeSignal.HOLD);
    }

    // ──────────────── 매도 신호 ────────────────

    @Test
    @DisplayName("OPEN 포지션이고 VOLUME 조건 충족 → SELL")
    void evaluate_returnsSell_whenOpenTradeAndVolumeConditionMet() {
        Strategy strategy = strategyWithVolumeSellConditionAndOpenTrade(50.0);
        List<CandleData> candles = List.of(candle(1000, 100.0));  // volume 100 >= threshold 50

        TradeSignal signal = evaluator.evaluate(strategy, candles);

        assertThat(signal).isEqualTo(TradeSignal.SELL);
    }

    @Test
    @DisplayName("OPEN 포지션이고 VOLUME 조건 미충족 → HOLD")
    void evaluate_returnsHold_whenOpenTradeAndVolumeConditionNotMet() {
        Strategy strategy = strategyWithVolumeSellConditionAndOpenTrade(200.0);
        List<CandleData> candles = List.of(candle(1000, 100.0));  // volume 100 < threshold 200

        TradeSignal signal = evaluator.evaluate(strategy, candles);

        assertThat(signal).isEqualTo(TradeSignal.HOLD);
    }
}