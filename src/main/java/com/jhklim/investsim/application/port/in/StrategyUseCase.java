package com.jhklim.investsim.application.port.in;

import com.jhklim.investsim.application.dto.CreateStrategyCommand;
import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.domain.model.Strategy;

import java.util.List;

public interface StrategyUseCase {
    List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition);
    List<Strategy> findByMember(Long memberId);
    void create(Long memberId, CreateStrategyCommand command);
    void activate(Long memberId, Long strategyId);
    void deactivate(Long memberId, Long strategyId);
}