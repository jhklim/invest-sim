package com.jhklim.investsim.site.upbit.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrentPriceStore {

    private final Map<String, Double> prices = new ConcurrentHashMap<>();

    public void update(String market, double price) {
        prices.put(market, price);
    }

    public double get(String market) {
        if (!prices.containsKey(market)) {
            throw new IllegalStateException("현재가 없음: " + market);
        }
        return prices.get(market);
    }

    public boolean exists(String market) {
        return prices.containsKey(market);
    }

}
