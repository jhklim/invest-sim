package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRepository strategyRepository;

    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        List<Strategy> strategies = strategyRepository.findActiveStrategiesByMarket(condition);
        return strategies;
    }
}
