package com.jhklim.investsim.adapter.out.persistence;

import com.jhklim.investsim.adapter.out.persistence.jpa.TradeRepository;
import com.jhklim.investsim.application.port.out.TradePort;
import com.jhklim.investsim.domain.model.PositionStatus;
import com.jhklim.investsim.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TradePersistenceAdapter implements TradePort {

    private final TradeRepository tradeRepository;

    @Override
    public Trade save(Trade trade) {
        return tradeRepository.save(trade);
    }

    @Override
    public Optional<Trade> findByStrategyIdAndPositionStatus(Long strategyId, PositionStatus status) {
        return tradeRepository.findByStrategyIdAndPositionStatus(strategyId, status);
    }

    @Override
    public List<Trade> findByMemberId(Long memberId) {
        return tradeRepository.findByMemberId(memberId);
    }
}