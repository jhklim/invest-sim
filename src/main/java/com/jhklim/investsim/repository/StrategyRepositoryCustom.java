package com.jhklim.investsim.repository;

import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;

import java.util.List;

public interface StrategyRepositoryCustom {
    List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition);
}
