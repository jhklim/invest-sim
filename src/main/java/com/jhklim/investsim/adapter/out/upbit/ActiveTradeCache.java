package com.jhklim.investsim.adapter.out.upbit;

import com.jhklim.investsim.application.port.out.ActiveTradePort;
import com.jhklim.investsim.domain.model.Trade;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveTradeCache implements ActiveTradePort {

    private final Map<Long, Trade> cache = new ConcurrentHashMap<>();

    @Override
    public void put(Long strategyId, Trade trade) {
        cache.put(strategyId, trade);
    }

    @Override
    public Optional<Trade> get(Long strategyId) {
        return Optional.ofNullable(cache.get(strategyId));
    }

    @Override
    public void remove(Long strategyId) {
        cache.remove(strategyId);
    }
}