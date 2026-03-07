package com.jhklim.investsim.adapter.out.persistence.jpa;

import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.domain.model.Strategy;

import java.util.List;

public interface StrategyRepositoryCustom {
    List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition);
}