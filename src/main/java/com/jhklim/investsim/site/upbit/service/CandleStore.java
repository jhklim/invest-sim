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
        store.put(market, new ArrayDeque<>(candles));
    }

    // add new candles (delete old candles)
    public void add(String market, CandleData candle) {
        Deque<CandleData> deque = store.computeIfAbsent(market, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(candle);
            if (deque.size() > MAX_SIZE) deque.pollFirst();
        }
    }

    // query candles list
    public List<CandleData> get(String market) {
        Deque<CandleData> deque = store.getOrDefault(market, new ArrayDeque<>());
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    // count candles (for RSI)
    public int size(String market) {
        Deque<CandleData> deque = store.getOrDefault(market, new ArrayDeque<>());
        synchronized (deque) {
            return deque.size();
        }
    }
}