package com.jhklim.investsim.application.service;

import com.jhklim.investsim.domain.model.Trade;
import com.jhklim.investsim.adapter.out.persistence.jpa.MemberRepository;
import com.jhklim.investsim.adapter.out.persistence.jpa.StrategyRepository;
import com.jhklim.investsim.adapter.out.persistence.jpa.TradeRepository;
import com.jhklim.investsim.application.dto.TradeOrderRequest;
import com.jhklim.investsim.domain.model.*;
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
        assertThat(trades.get(0).getOpenPricePerShare()).isEqualByComparingTo(new BigDecimal("5000000"));
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

        // activate: 투자금 예약 (잔고 100,000 → 50,000)
        member.deductBalance(strategy.getBuyAmount());

        // 매수 체결 (openAmount = 0.01 * 5,000,000 = 50,000)
        TradeOrderRequest order = new TradeOrderRequest(new BigDecimal("5000000"), new BigDecimal("0.01"));
        tradeService.buy(strategy, order);

        // when
        BigDecimal sellPrice = new BigDecimal("6000000");
        tradeService.sell(strategy, sellPrice);

        // then
        // returnAmount = openQuantity(0.01) * sellPrice(6,000,000) = 60,000
        // openAmount   = openQuantity(0.01) * openPrice(5,000,000) = 50,000
        Trade trade = tradeRepository.findAll().get(0);
        assertThat(trade.getPositionStatus()).isEqualTo(PositionStatus.CLOSE);
        assertThat(trade.getCloseAmount()).isEqualByComparingTo(new BigDecimal("60000"));
        assertThat(trade.getProfitAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(member.getBalance()).isEqualByComparingTo(new BigDecimal("110000"));
    }

    @Test
    @DisplayName("멤버 ID로 거래 목록 조회 시 해당 멤버의 TradeResponse 반환")
    void findByMember_returnsTrades() {
        // given
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", new BigDecimal("100000"));
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", new BigDecimal("50000"));
        strategyRepository.save(strategy);

        TradeOrderRequest order = new TradeOrderRequest(new BigDecimal("5000000"), new BigDecimal("0.01"));
        tradeService.buy(strategy, order);

        // when
        List<Trade> result = tradeService.findByMember(member.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStrategy().getId()).isEqualTo(strategy.getId());
        assertThat(result.get(0).getOpenPricePerShare()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(result.get(0).getPositionStatus()).isEqualTo(PositionStatus.OPEN);
    }

    @Test
    @DisplayName("거래가 없는 멤버 조회 시 빈 리스트 반환")
    void findByMember_returnsEmptyList() {
        // given
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", new BigDecimal("100000"));
        memberRepository.save(member);

        // when
        List<Trade> result = tradeService.findByMember(member.getId());

        // then
        assertThat(result).isEmpty();
    }
}