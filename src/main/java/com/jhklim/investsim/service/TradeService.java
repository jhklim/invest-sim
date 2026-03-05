package com.jhklim.investsim.service;

import com.jhklim.investsim.controller.dto.trade.CreateTradeRequest;
import com.jhklim.investsim.controller.dto.trade.TradeResponse;
import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.PositionStatus;
import com.jhklim.investsim.domain.Trade;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.TradeOrderRequest;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import com.jhklim.investsim.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;

    @Transactional
    public void buy(Strategy strategy, TradeOrderRequest order) {
        Member member = strategy.getMember();

        Trade trade = new Trade(member, strategy, order);
        tradeRepository.save(trade);
        log.info("[BUY] 전략: {} / 마켓: {} / 가격: {} / 수량: {}",
                strategy.getName(), strategy.getMarket(),
                order.getPrice(), order.getQuantity());
    }

    @Transactional
    public void sell(Strategy strategy, BigDecimal currentPrice) {
        Trade trade = tradeRepository.findByStrategyIdAndPositionStatus(strategy.getId(), PositionStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("매도할 포지션이 없습니다. strategyId=" + strategy.getId()));

        BigDecimal returnAmount = trade.getOpenQuantity().multiply(currentPrice);
        trade.getMember().addBalance(returnAmount);
        trade.close(returnAmount);

        log.info("[SELL] 전략: {} / 마켓: {} / 매도가: {} / 수량: {} / 수익: {}",
                strategy.getName(), strategy.getMarket(),
                currentPrice, trade.getOpenQuantity(), trade.getProfitAmount());
    }

    @Transactional
    public void create(Long memberId, CreateTradeRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Strategy strategy = strategyRepository.findById(request.getStrategyId())
                .orElseThrow(() -> new IllegalArgumentException("해당 전략이 존재하지 않습니다."));

        if (!strategy.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 전략에 대한 권한이 없습니다.");
        }

        Trade trade = new Trade(member, strategy, strategy.getBuyAmount());
        tradeRepository.save(trade);
    }

    public List<TradeResponse> findByMember(Long memberId) {
        return tradeRepository.findByMemberId(memberId).stream()
                .map(TradeResponse::from)
                .toList();
    }
}