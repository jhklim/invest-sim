package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    // 전략 활성화 - 잔고 차감(묶기)
    @Transactional
    public void activate(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("전략이 존재하지 않습니다."));

        try {
            Member member = strategy.getMember();
            member.deductBalance(strategy.getBuyAmount());
            strategy.activate();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("잔고 처리 중 충돌이 발생했습니다. 다시 시도해주세요.");
        }
    }
}
