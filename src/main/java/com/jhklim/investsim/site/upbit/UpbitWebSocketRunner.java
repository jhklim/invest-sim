package com.jhklim.investsim.site.upbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.site.upbit.dto.CandleData;
import com.jhklim.investsim.site.upbit.dto.TradeTickData;
import com.jhklim.investsim.site.upbit.service.CandleStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitWebSocketRunner {

    private final UpbitRestClient upbitRestClient;
    private final CandleStore candleStore;
    private final ObjectMapper objectMapper;

    private static final String MARKET = "KRW-BTC";

    @PostConstruct
    public void start() throws URISyntaxException {

        // step 1. add initial candles
        List<CandleData> candles = upbitRestClient.getCandles(MARKET, 50);
        candleStore.init(MARKET, candles);
        log.info("초기 캔들 적재 완료 - {}개", candles.size());

        // step 2. connect WebSocket
        URI uri = new URI("wss://api.upbit.com/websocket/v1");

        WebSocketClient client = new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                log.info("WebSocket Connected");

                String subscribeMsg = "[" +
                        "{\"ticket\":\"uuid_\"}," +
                        "{\"type\":\"trade\", \"codes\":[\"" + MARKET + "\"]}]";

                send(subscribeMsg);
            }

            @Override
            public void onMessage(String s) {
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                String jsonString = StandardCharsets.UTF_8.decode(bytes).toString();
                try {
                    // JSON String → Java 객체
                    TradeTickData tick = objectMapper.readValue(jsonString, TradeTickData.class);
                    log.info("[{}] 체결가: {} / 수량: {} / {}",
                            tick.getMarket(), tick.getTradePrice(),
                            tick.getTradeVolume(), tick.getAskBid());
                } catch (Exception e) {
                    log.error("Parsing Error: {}", jsonString, e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.warn("WebSocket Closed - code: {}, reason: {}", code, reason);
            }

            @Override
            public void onError(Exception e) {
                log.error("WebSocket Error", e);
            }
        };

        client.connect();
    }
}
