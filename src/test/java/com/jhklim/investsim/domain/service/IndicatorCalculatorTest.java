package com.jhklim.investsim.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.domain.model.CandleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorCalculatorTest {

    private IndicatorCalculator calculator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        calculator = new IndicatorCalculator();
        objectMapper = new ObjectMapper();
    }

    // CandleData는 생성자/setter가 없고 Jackson 역직렬화로만 생성 가능
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

    private List<CandleData> candles(double... prices) {
        List<CandleData> list = new ArrayList<>();
        for (double price : prices) {
            list.add(candle(price, 100.0));
        }
        return list;
    }

    // ──────────────── RSI ────────────────

    @Test
    @DisplayName("RSI: 캔들이 15개(RSI_PERIOD+1) 미만이면 empty 반환")
    void calculateRsi_returnsEmpty_whenCandlesInsufficient() {
        List<CandleData> tooFew = candles(1000, 2000, 3000);

        assertThat(calculator.calculateRsi(tooFew)).isEmpty();
    }

    @Test
    @DisplayName("RSI: 모든 캔들이 상승하면 하락이 없으므로 RSI = 100")
    void calculateRsi_returns100_whenAllCandlesGoUp() {
        // RSI_PERIOD=14 이므로 최소 15개 필요
        double[] prices = {1000, 1100, 1200, 1300, 1400, 1500, 1600,
                           1700, 1800, 1900, 2000, 2100, 2200, 2300, 2400};

        Optional<BigDecimal> result = calculator.calculateRsi(candles(prices));

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("RSI: 상승/하락이 섞인 정상 케이스에서 0~100 사이 값 반환")
    void calculateRsi_returnsValueBetween0And100_whenMixedMoves() {
        double[] prices = {1000, 1100, 900, 1200, 800, 1300, 700,
                           1400, 600, 1500, 500, 1600, 400, 1700, 300};

        Optional<BigDecimal> result = calculator.calculateRsi(candles(prices));

        assertThat(result).isPresent();
        assertThat(result.get())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO)
                .isLessThanOrEqualTo(BigDecimal.valueOf(100));
    }

    // ──────────────── MA ────────────────

    @Test
    @DisplayName("MA: 캔들이 period 미만이면 empty 반환")
    void calculateMa_returnsEmpty_whenCandlesInsufficient() {
        List<CandleData> tooFew = candles(1000, 2000);

        assertThat(calculator.calculateMa(tooFew, 5)).isEmpty();
    }

    @Test
    @DisplayName("MA: 최근 N개 종가의 평균을 정확히 반환")
    void calculateMa_returnsCorrectAverage() {
        // 캔들 5개, period=3 → 최근 3개(300, 400, 500)의 평균 = 400
        List<CandleData> candleList = candles(100, 200, 300, 400, 500);

        Optional<BigDecimal> result = calculator.calculateMa(candleList, 3);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("400.00000000"));
    }

    @Test
    @DisplayName("MA: 캔들 수와 period가 동일하면 전체 평균 반환")
    void calculateMa_returnsAverage_whenCandleCountEqualsPeriod() {
        // 3개 캔들, period=3 → 평균 200
        List<CandleData> candleList = candles(100, 200, 300);

        Optional<BigDecimal> result = calculator.calculateMa(candleList, 3);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("200.00000000"));
    }
}