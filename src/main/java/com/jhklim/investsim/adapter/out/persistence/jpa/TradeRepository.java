package com.jhklim.investsim.adapter.out.persistence.jpa;

import com.jhklim.investsim.domain.model.PositionStatus;
import com.jhklim.investsim.domain.model.Trade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Optional<Trade> findByStrategyIdAndPositionStatus(Long strategyId, PositionStatus status);

    @EntityGraph(attributePaths = {"strategy"})
    List<Trade> findByMemberId(Long memberId);

    @EntityGraph(attributePaths = {"strategy", "strategy.member"})
    List<Trade> findAllByPositionStatus(PositionStatus positionStatus);
}