package com.jhklim.investsim.application.port.out;

import com.jhklim.investsim.domain.model.Exchange;
import com.jhklim.investsim.domain.model.Strategy;

import java.util.List;

public interface ActiveStrategyPort {
    void add(Strategy strategy);
    void remove(Long strategyId);
    List<Strategy> findByMarket(Exchange exchange, String market);
}