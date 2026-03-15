package com.jhklim.investsim.application.service;

import com.jhklim.investsim.application.port.in.TradeUseCase;
import com.jhklim.investsim.application.port.out.TradePort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import com.jhklim.investsim.application.dto.TradeOrderRequest;
import com.jhklim.investsim.domain.model.Member;
import com.jhklim.investsim.domain.model.PositionStatus;
import com.jhklim.investsim.domain.model.Strategy;
import com.jhklim.investsim.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TradeService implements TradeUseCase {

    private final TradePort tradePort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Trade buy(Strategy strategy, TradeOrderRequest order) {
        Member member = strategy.getMember();

        Trade trade = new Trade(member, strategy, order);
        tradePort.save(trade);
        log.info("[BUY] 전략: {} / 마켓: {} / 가격: {} / 수량: {}",
                strategy.getName(), strategy.getMarket(),
                order.getPrice(), order.getQuantity());
        return trade;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sell(Strategy strategy, BigDecimal currentPrice) {
        Trade trade = tradePort.findByStrategyIdAndPositionStatus(strategy.getId(), PositionStatus.OPEN)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_OPEN_POSITION));

        BigDecimal returnAmount = trade.getOpenQuantity().multiply(currentPrice);
        trade.getMember().addBalance(returnAmount);
        trade.close(returnAmount);

        log.info("[SELL] 전략: {} / 마켓: {} / 매도가: {} / 수량: {} / 수익: {}",
                strategy.getName(), strategy.getMarket(),
                currentPrice, trade.getOpenQuantity(), trade.getProfitAmount());
    }


    public List<Trade> findByMember(Long memberId) {
        return tradePort.findByMemberId(memberId);
    }
}