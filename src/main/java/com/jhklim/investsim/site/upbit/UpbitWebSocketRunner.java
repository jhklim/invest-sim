package com.jhklim.investsim.site.upbit;

import com.jhklim.investsim.site.upbit.dto.CandleData;
import com.jhklim.investsim.site.upbit.service.CandleStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 시작 시 초기화
 * - 초기 캔들 데이터 적재
 * - WebSocket 연결 시작
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitWebSocketRunner {

    private final UpbitRestClient upbitRestClient;
    private final CandleStore candleStore;
    private final UpbitWebSocketClient webSocketClient;

    private static final String MARKET = "KRW-BTC";

    @PostConstruct
    public void start() {
        List<CandleData> candles = upbitRestClient.getCandles(MARKET, 50);
        candleStore.init(MARKET, candles);
        log.info("초기 캔들 적재 완료 - {}개", candles.size());

        webSocketClient.connect();
    }
}