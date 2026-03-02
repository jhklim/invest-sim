package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.*;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.TradeOrderRequest;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import com.jhklim.investsim.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class TradeServiceTest {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StrategyRepository strategyRepository;

    @Test
    @DisplayName("정상 매수 시 Trade가 DB에 저장")
    public void buy_trade() {
        // given
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", new BigDecimal("100000"));
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", new BigDecimal("50000"));
        strategyRepository.save(strategy);

        TradeOrderRequest order = new TradeOrderRequest(new BigDecimal("5000000"), new BigDecimal("0.01"));

        // when
        tradeService.buy(strategy, order);

        // then
        List<Trade> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getOpenPrice()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(trades.get(0).getOpenQuantity()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(trades.get(0).getPositionStatus()).isEqualTo(PositionStatus.OPEN);
    }
}