package com.jhklim.investsim.application.port.out;

import com.jhklim.investsim.domain.model.Trade;

import java.util.Optional;

public interface ActiveTradePort {
    void put(Long strategyId, Trade trade);
    Optional<Trade> get(Long strategyId);
    void remove(Long strategyId);
}