package com.jhklim.investsim.service;

import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.Role;
import com.jhklim.investsim.domain.strategy.BuyStrategy;
import com.jhklim.investsim.domain.strategy.Indicator;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.repository.MemberRepository;
import com.jhklim.investsim.repository.StrategyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

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
        Member member = new Member("test@gmail.com", "1234", Role.USER, "tester", new BigDecimal("100000"));
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", new BigDecimal("50000"));
        strategy.getBuyStrategies().add(new BuyStrategy(strategy, Indicator.RSI, 30));
        strategyRepository.save(strategy);

        // when
        strategyService.activate(strategy.getId());

        // then
        assertThat(member.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(strategy.isActive()).isTrue();
    }

    @Test
    @DisplayName("잔고 부족 시 전략 활성화에 실패하고 잔고는 유지")
    public void activate_rollbackWhenInsufficientBalance() {
        // given
        Member member = new Member("test2@gmail.com", "1234", Role.USER, "tester2", new BigDecimal("100000"));
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", new BigDecimal("500000"));
        strategyRepository.save(strategy);

        // when
        assertThatThrownBy(() -> strategyService.activate(strategy.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔고 부족");

        // then
        assertThat(member.getBalance()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(strategy.isActive()).isFalse();
    }

    @Test
    @DisplayName("매수 체결 전 전략 비활성화 시 투자금액이 전액 환불")
    public void deactivate_refundsBuyAmountBeforeTradeOpen() {
        // given
        Member member = new Member("test3@gmail.com", "1234", Role.USER, "tester3", new BigDecimal("100000"));
        memberRepository.save(member);

        Strategy strategy = new Strategy(member, Exchange.UPBIT, "KRW-BTC", new BigDecimal("50000"));
        strategyRepository.save(strategy);

        strategyService.activate(strategy.getId());

        // when
        strategyService.deactivate(strategy.getId());

        // then
        assertThat(member.getBalance()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(strategy.isActive()).isFalse();
    }
}