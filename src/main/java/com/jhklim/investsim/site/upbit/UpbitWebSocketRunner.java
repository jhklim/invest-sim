package com.jhklim.investsim.site.upbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.dto.TradeOrderRequest;
import com.jhklim.investsim.service.StrategyService;
import com.jhklim.investsim.service.TradeService;
import com.jhklim.investsim.site.upbit.dto.CandleData;
import com.jhklim.investsim.site.upbit.dto.TradeTickData;
import com.jhklim.investsim.site.upbit.service.CandleStore;
import com.jhklim.investsim.site.upbit.service.CurrentPriceStore;
import com.jhklim.investsim.site.upbit.service.StrategyEvaluator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitWebSocketRunner {

    private final UpbitRestClient upbitRestClient;
    private final CandleStore candleStore;
    private final ObjectMapper objectMapper;
    private final StrategyEvaluator strategyEvaluator;
    private final StrategyService strategyService;
    private final TradeService tradeService;
    private final CurrentPriceStore currentPriceStore;

    private static final String MARKET = "KRW-BTC";
    private static final int MAX_RETRY_DELAY_SECONDS = 60;

    private WebSocketClient client;
    private int retryCount = 0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void start() {
        List<CandleData> candles = upbitRestClient.getCandles(MARKET, 50);
        candleStore.init(MARKET, candles);
        log.info("초기 캔들 적재 완료 - {}개", candles.size());

        connect();
    }

    private void connect() {
        try {
            client = createClient();
            client.connect();
        } catch (Exception e) {
            log.error("[WebSocket] 연결 실패", e);
            scheduleReconnect();
        }
    }

    private WebSocketClient createClient() throws Exception {
        URI uri = new URI("wss://api.upbit.com/websocket/v1");

        return new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                retryCount = 0;
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
                    onTick(tick);
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

    private void scheduleReconnect() {
        long delaySeconds = Math.min((long) Math.pow(2, retryCount), MAX_RETRY_DELAY_SECONDS);
        retryCount++;
        log.info("[WebSocket] {}초 후 재연결 시도 ({}회차)", delaySeconds, retryCount);
        scheduler.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private void onTick(TradeTickData tick) {
        currentPriceStore.update(tick.getMarket(), tick.getTradePrice());
        List<CandleData> candles = candleStore.get(tick.getMarket());

        ExchangeMarketSearchCond condition = new ExchangeMarketSearchCond(Exchange.UPBIT, tick.getMarket());
        List<Strategy> activeStrategies = strategyService.findActiveStrategiesByMarket(condition);

        for (Strategy strategy : activeStrategies) {
            TradeSignal signal = strategyEvaluator.evaluate(strategy, candles);
            log.info("[{}] 전략: {} / 신호: {}", tick.getMarket(), strategy.getName(), signal);

            TradeOrderRequest order = new TradeOrderRequest(tick.getTradePrice(), calculateQuantity(strategy, tick.getTradePrice()));

            if (signal == TradeSignal.BUY) {
                try {
                    tradeService.buy(strategy, order);
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("[BUY] 낙관적 락 충돌 - 전략: {}", strategy.getName());
                }
            } else if (signal == TradeSignal.SELL) {
                try {
                    tradeService.sell(strategy, tick.getTradePrice());
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.warn("[SELL] 낙관적 락 충돌 - 전략: {}", strategy.getName());
                }
            }
        }
    }

    private BigDecimal calculateQuantity(Strategy strategy, BigDecimal tradePrice) {
        return strategy.getBuyAmount().divide(tradePrice, 8, RoundingMode.HALF_UP);
    }
}