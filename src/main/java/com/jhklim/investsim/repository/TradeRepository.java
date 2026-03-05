package com.jhklim.investsim.repository;

import com.jhklim.investsim.domain.PositionStatus;
import com.jhklim.investsim.domain.Trade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Optional<Trade> findByStrategyIdAndPositionStatus(Long strategyId, PositionStatus status);

    @EntityGraph(attributePaths = {"strategy"})
    List<Trade> findByMemberId(Long memberId);
}
