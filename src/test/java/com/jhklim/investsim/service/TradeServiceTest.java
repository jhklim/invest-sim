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

    @Test
    @DisplayName("매도 시 Trade가 CLOSE 상태로 변경되고 잔고 변경")
    public void sell_closesTradeAndRestoresBalance() {
        // given
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", new BigDecimal("100000"));
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", new BigDecimal("50000"));
        strategyRepository.save(strategy);

        // 매수 체결
        TradeOrderRequest order = new TradeOrderRequest(new BigDecimal("5000000"), new BigDecimal("0.01"));
        tradeService.buy(strategy, order);

        // when
        BigDecimal sellPrice = new BigDecimal("6000000");
        tradeService.sell(strategy, sellPrice);

        // then
        Trade trade = tradeRepository.findAll().get(0);
        assertThat(trade.getPositionStatus()).isEqualTo(PositionStatus.CLOSE);
        assertThat(trade.getClosePrice()).isEqualByComparingTo(sellPrice);
        assertThat(trade.getProfitAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(member.getBalance()).isEqualByComparingTo(new BigDecimal("110000"));
    }
}