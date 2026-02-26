package com.jhklim.investsim.site.upbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.service.StrategyService;
import com.jhklim.investsim.site.upbit.dto.CandleData;
import com.jhklim.investsim.site.upbit.dto.TradeTickData;
import com.jhklim.investsim.site.upbit.service.CandleStore;
import com.jhklim.investsim.site.upbit.service.StrategyEvaluator;
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
    private final StrategyEvaluator strategyEvaluator;
    private final StrategyService strategyService;

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
                    // JSON String -> Java 객체
                    TradeTickData tick = objectMapper.readValue(jsonString, TradeTickData.class);
                    onTick(tick);
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

    private void onTick(TradeTickData tick) {
        List<CandleData> candles = candleStore.get(tick.getMarket());

        ExchangeMarketSearchCond condition = new ExchangeMarketSearchCond(Exchange.UPBIT, tick.getMarket());
        List<Strategy> activeStrategies = strategyService.findActiveStrategiesByMarket(condition);

        for (Strategy strategy : activeStrategies) {
            TradeSignal signal = strategyEvaluator.evaluate(strategy, candles);
            log.info("[{}] 전략: {} / 신호: {}", tick.getMarket(), strategy.getName(), signal);

            // TODO: TradeService 연결 (매매 체결)
        }
    }
}
