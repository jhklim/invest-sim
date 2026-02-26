package com.jhklim.investsim.site.upbit.service;

import com.jhklim.investsim.site.upbit.dto.CandleData;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CandleStore {

    private static final int MAX_SIZE = 50;

    // save by type (ex: "KRW-BTC" → [CANDLE1, CANDLE2, ...])
    private final Map<String, Deque<CandleData>> store = new ConcurrentHashMap<>();

    // add initial candles (1 time each starting the program)
    public void init(String market, List<CandleData> candles) {
        Deque<CandleData> deque = new ArrayDeque<>(candles);
        store.put(market, deque);
    }

    // add new candles (delete old candles)
    public void add(String market, CandleData candle) {
        store.computeIfAbsent(market, k -> new ArrayDeque<>());
        Deque<CandleData> deque = store.get(market);
        deque.addLast(candle);
        if (deque.size() > MAX_SIZE) deque.pollFirst();
    }

    // query candles list
    public List<CandleData> get(String market) {
        return new ArrayList<>(store.getOrDefault(market, new ArrayDeque<>()));
    }

    // count candles (for RSI)
    public int size(String market) {
        return store.getOrDefault(market, new ArrayDeque<>()).size();
    }
}
