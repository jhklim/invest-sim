package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.Role;
import com.jhklim.investsim.domain.strategy.BuyStrategy;
import com.jhklim.investsim.domain.strategy.Indicator;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StrategyServiceTest {
    @Autowired
    private StrategyService strategyService;

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("전략 활성시 시 잔고 차감 후 DB에 반영")
    public void activate_persistsToDatabase() {
        // given
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", 1000000);
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", 500000.0);
        strategy.getBuyStrategies().add(new BuyStrategy(strategy, Indicator.RSI, 30));
        strategyRepository.save(strategy);

        // when
        strategyService.activate(strategy.getId());

        // then
        assertThat(member.getBalance()).isEqualTo(500000);
        assertThat(strategy.isActive()).isTrue();
    }
}