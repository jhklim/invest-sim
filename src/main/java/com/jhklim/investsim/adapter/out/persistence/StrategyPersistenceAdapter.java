package com.jhklim.investsim.adapter.out.persistence;

import com.jhklim.investsim.adapter.out.persistence.jpa.StrategyRepository;
import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.application.port.out.StrategyPort;
import com.jhklim.investsim.domain.model.Strategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StrategyPersistenceAdapter implements StrategyPort {

    private final StrategyRepository strategyRepository;

    @Override
    public Strategy save(Strategy strategy) {
        return strategyRepository.save(strategy);
    }

    @Override
    public Optional<Strategy> findById(Long id) {
        return strategyRepository.findById(id);
    }

    @Override
    public List<Strategy> findByMemberId(Long memberId) {
        return strategyRepository.findByMemberId(memberId);
    }

    @Override
    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        return strategyRepository.findActiveStrategiesByMarket(condition);
    }

    @Override
    public List<Strategy> findAllActive() {
        return strategyRepository.findAllByIsActiveTrue();
    }
}