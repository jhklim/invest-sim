package com.jhklim.investsim.site.upbit.service;

import com.jhklim.investsim.site.upbit.dto.CandleData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IndicatorCalculator {

    private static final int RSI_PERIOD = 14;
    private static final int SCALE = 8;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    // 업비트 기준 가중치: a = 1 / (1 + (day - 1)) = 1 / day
    private static final BigDecimal ALPHA = BigDecimal.ONE
            .divide(BigDecimal.valueOf(RSI_PERIOD), SCALE, ROUNDING);

    /**
     * RSI 계산 (지수 이동평균 방식 - 업비트 기준)
     * a = 1 / (1 + (day - 1))
     * @param candles 오래된 순 → 최신 순 정렬된 캔들 리스트
     * @return RSI 값 (0~100), 캔들 부족 시 Optional.empty() 반환
     */
    public Optional<BigDecimal> calculateRsi(List<CandleData> candles) {
        if (candles.size() < RSI_PERIOD + 1) return Optional.empty();

        List<BigDecimal> upList = new ArrayList<>();
        List<BigDecimal> downList = new ArrayList<>();

        for (int i = 0; i < candles.size() - 1; i++) {
            BigDecimal gap = candles.get(i + 1).getClosePrice()
                    .subtract(candles.get(i).getClosePrice());
            int sign = gap.compareTo(BigDecimal.ZERO);
            if (sign > 0) {
                upList.add(gap);
                downList.add(BigDecimal.ZERO);
            } else if (sign < 0) {
                upList.add(BigDecimal.ZERO);
                downList.add(gap.negate());
            } else {
                upList.add(BigDecimal.ZERO);
                downList.add(BigDecimal.ZERO);
            }
        }

        BigDecimal oneMinusAlpha = BigDecimal.ONE.subtract(ALPHA);

        // AU (상승 EMA)
        BigDecimal upEma = upList.get(0);
        for (int i = 1; i < upList.size(); i++) {
            upEma = upList.get(i).multiply(ALPHA)
                    .add(upEma.multiply(oneMinusAlpha));
        }

        // AD (하락 EMA)
        BigDecimal downEma = downList.get(0);
        for (int i = 1; i < downList.size(); i++) {
            downEma = downList.get(i).multiply(ALPHA)
                    .add(downEma.multiply(oneMinusAlpha));
        }

        if (downEma.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of(BigDecimal.valueOf(100));
        }

        BigDecimal rs = upEma.divide(downEma, SCALE, ROUNDING);
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal rsi = hundred.subtract(
                hundred.divide(BigDecimal.ONE.add(rs), SCALE, ROUNDING)
        );
        return Optional.of(rsi);
    }

    /**
     * 단순 이동평균(MA) 계산
     * @param candles 오래된 순 → 최신 순 정렬된 캔들 리스트
     * @param period  기간 (예: 5, 20)
     * @return MA 값, 캔들 부족 시 Optional.empty() 반환
     */
    public Optional<BigDecimal> calculateMa(List<CandleData> candles, int period) {
        if (candles.size() < period) return Optional.empty();

        List<CandleData> recent = candles.subList(candles.size() - period, candles.size());
        BigDecimal sum = recent.stream()
                .map(CandleData::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING));
    }
}