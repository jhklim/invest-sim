package com.jhklim.investsim.adapter.in.websocket;

import com.jhklim.investsim.adapter.out.upbit.CandleStore;
import com.jhklim.investsim.adapter.out.upbit.UpbitRestClient;
import com.jhklim.investsim.domain.model.CandleData;
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