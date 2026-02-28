package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.*;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.TradeOrderRequest;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import com.jhklim.investsim.repository.TradeRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", 100000);
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", 50000.0);
        strategyRepository.save(strategy);

        TradeOrderRequest order = new TradeOrderRequest(5000000.0, 0.01);

        // when
        tradeService.buy(strategy, order);

        // then
        List<Trade> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getOpenPrice()).isEqualTo(5000000.0);
        assertThat(trades.get(0).getOpenQuantity()).isEqualTo(0.01);
        assertThat(trades.get(0).getPositionStatus()).isEqualTo(PositionStatus.OPEN);
    }
}
