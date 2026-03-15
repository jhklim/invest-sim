package com.jhklim.investsim.application.port.out;

import com.jhklim.investsim.domain.model.PositionStatus;
import com.jhklim.investsim.domain.model.Trade;

import java.util.List;
import java.util.Optional;

public interface TradePort {
    Trade save(Trade trade);
    Optional<Trade> findByStrategyIdAndPositionStatus(Long strategyId, PositionStatus status);
    List<Trade> findByMemberId(Long memberId);
    List<Trade> findAllByPositionStatus(PositionStatus status);
}