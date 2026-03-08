package com.jhklim.investsim.application.port.in;

import com.jhklim.investsim.application.dto.TradeOrderRequest;
import com.jhklim.investsim.domain.model.Strategy;
import com.jhklim.investsim.domain.model.Trade;

import java.math.BigDecimal;
import java.util.List;

public interface TradeUseCase {
    void buy(Strategy strategy, TradeOrderRequest order);
    void sell(Strategy strategy, BigDecimal currentPrice);
    List<Trade> findByMember(Long memberId);
}