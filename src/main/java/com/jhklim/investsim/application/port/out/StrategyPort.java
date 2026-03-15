package com.jhklim.investsim.application.port.out;

import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.domain.model.Strategy;

import java.util.List;
import java.util.Optional;

public interface StrategyPort {
    Strategy save(Strategy strategy);
    Optional<Strategy> findById(Long id);
    List<Strategy> findByMemberId(Long memberId);
    List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition);
    List<Strategy> findAllActive();
    long countByMemberId(Long memberId);
}