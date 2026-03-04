package com.jhklim.investsim.site.upbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.site.upbit.dto.TradeTickData;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Upbit WebSocket 연결을 관리
 * - 연결 수립 및 구독 메시지 전송
 * - 연결 종료 시 지수 백오프(Exponential Backoff)로 재연결 스케줄링
 * - 수신 메시지 파싱 후 TickProcessor에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitWebSocketClient {

    private final ObjectMapper objectMapper;
    private final TickProcessor tickProcessor;

    private static final String MARKET = "KRW-BTC";
    private static final int MAX_RETRY_DELAY_SECONDS = 60;

    private WebSocketClient client;

    // onOpen(WebSocket 스레드)과 scheduleReconnect(scheduler 스레드)에서 동시에 접근하므로 가시성 보장을 위해 volatile 선언
    private volatile int retryCount = 0;

    // 재연결 스케줄링 전용 스레드 1개
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void connect() {
        try {
            client = createClient();
            client.connect();
        } catch (Exception e) {
            log.error("[WebSocket] 연결 실패", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        long delaySeconds = Math.min((long) Math.pow(2, retryCount), MAX_RETRY_DELAY_SECONDS);
        retryCount++;
        log.info("[WebSocket] {}초 후 재연결 시도 ({}회차)", delaySeconds, retryCount);
        scheduler.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private WebSocketClient createClient() throws Exception {
        URI uri = new URI("wss://api.upbit.com/websocket/v1");

        return new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                retryCount = 0; // 연결 성공 시 초기화
                log.info("[WebSocket] 연결됨");

                String subscribeMsg = "[" +
                        "{\"ticket\":\"investsim\"}," +
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
                    TradeTickData tick = objectMapper.readValue(jsonString, TradeTickData.class);
                    tickProcessor.process(tick);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    log.error("[WebSocket] JSON 파싱 오류: {}", jsonString, e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.warn("[WebSocket] 연결 종료 - code: {}, reason: {}", code, reason);
                scheduleReconnect();
            }

            @Override
            public void onError(Exception e) {
                log.error("[WebSocket] 오류 발생", e);
            }
        };
    }

    // 앱 종료 시 scheduler 스레드를 정리
    // @PreDestroy: Spring 컨테이너가 Bean을 소멸시키기 전에 호출되는 메서드
    @PreDestroy
    public void shutdown() {
        log.info("[WebSocket] scheduler 종료");
        scheduler.shutdown();
    }
}