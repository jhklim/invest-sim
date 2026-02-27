package com.jhklim.investsim.site.upbit.service;

import com.jhklim.investsim.site.upbit.dto.CandleData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IndicatorCalculator {

    private static final int RSI_PERIOD = 14;

    /**
     * RSI 계산 (지수 이동평균 방식 - 업비트 기준)
     * a = 1 / (1 + (day - 1))
     * @param candles 오래된 순 → 최신 순 정렬된 캔들 리스트
     * @return RSI 값 (0~100), 캔들 부족 시 Optional.empty() 반환
     */
    public Optional<Double> calculateRsi(List<CandleData> candles) {
        if (candles.size() < RSI_PERIOD + 1) return Optional.empty();

        // 상승/하락 리스트 구성
        List<Double> upList = new ArrayList<>();
        List<Double> downList = new ArrayList<>();

        for (int i = 0; i < candles.size() - 1; i++) {
            double gap = candles.get(i + 1).getClosePrice() - candles.get(i).getClosePrice();
            if (gap > 0) {
                upList.add(gap);
                downList.add(0.0);
            } else if (gap < 0) {
                upList.add(0.0);
                downList.add(gap * -1);
            } else {
                upList.add(0.0);
                downList.add(0.0);
            }
        }

        // 업비트 기준 가중치: a = 1 / (1 + (day - 1))
        double a = 1.0 / (1 + (RSI_PERIOD - 1));

        // AU (상승 EMA)
        double upEma = upList.get(0);
        for (int i = 1; i < upList.size(); i++) {
            upEma = (upList.get(i) * a) + (upEma * (1 - a));
        }

        // AD (하락 EMA)
        double downEma = downList.get(0);
        for (int i = 1; i < downList.size(); i++) {
            downEma = (downList.get(i) * a) + (downEma * (1 - a));
        }

        if (downEma == 0) return Optional.of(100.0);

        double rs = upEma / downEma;
        return Optional.of(100 - (100 / (1 + rs)));
    }

    /**
     * 단순 이동평균(MA) 계산
     * @param candles 오래된 순 → 최신 순 정렬된 캔들 리스트
     * @param period  기간 (예: 5, 20)
     * @return MA 값, 캔들 부족 시 -1 반환
     */
    public double calculateMa(List<CandleData> candles, int period) {
        if (candles.size() < period) return -1;

        List<CandleData> recent = candles.subList(candles.size() - period, candles.size());
        return recent.stream()
                .mapToDouble(CandleData::getClosePrice)
                .average()
                .orElse(-1);
    }
}