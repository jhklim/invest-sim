package com.jhklim.investsim.adapter.out.upbit;

import com.jhklim.investsim.application.port.out.ActiveStrategyPort;
import com.jhklim.investsim.domain.model.Exchange;
import com.jhklim.investsim.domain.model.Strategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ActiveStrategyCache implements ActiveStrategyPort {

    private final Map<String, CopyOnWriteArrayList<Strategy>> cache = new ConcurrentHashMap<>();

    @Override
    public void add(Strategy strategy) {
        String key = toKey(strategy.getExchange(), strategy.getMarket());
        cache.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(strategy);
    }

    @Override
    public void remove(Long strategyId) {
        cache.values().forEach(list -> list.removeIf(s -> s.getId().equals(strategyId)));
    }

    @Override
    public List<Strategy> findByMarket(Exchange exchange, String market) {
        return cache.getOrDefault(toKey(exchange, market), new CopyOnWriteArrayList<>());
    }

    private String toKey(Exchange exchange, String market) {
        return exchange.name() + ":" + market;
    }
}