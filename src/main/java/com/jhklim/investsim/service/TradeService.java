package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.Trade;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.TradeOrderRequest;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public void buy(Strategy strategy, TradeOrderRequest order) {
        Member member = strategy.getMember();

        try {
           member.deductBalance(order.getTotalOrderPrice());
           Trade trade = new Trade(member, strategy, order);
           tradeRepository.save(trade);
           log.info("[BUY] 전략: {} / 마켓: {} / 가격: {} / 수량: {}",
                   strategy.getName(), strategy.getMarket(),
                   order.getPrice(), order.getQuantity());
       } catch (ObjectOptimisticLockingFailureException e) {
           log.warn("[BUY] 낙관적 락 충돌 - 전략: {}", strategy.getName());
       }
    }
}
