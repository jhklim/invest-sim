package com.jhklim.investsim.domain;

import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.TradeOrderRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends BaseTimeEntity {

    @Id @GeneratedValue
    @Column(name = "trade_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private double openPrice;
    private double openQuantity;

    private double closePrice;
    private double profitAmount;
    private double profitRate;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @Enumerated(STRING)
    private PositionStatus positionStatus; // [OPEN, CLOSE]

    public double getTotalOpenPrice() {
        return this.getOpenPrice() * getOpenQuantity();
    }

    public void close(double closePrice) {
        this.positionStatus = PositionStatus.CLOSE;
        this.closePrice = closePrice;
        this.profitAmount = (closePrice - this.openPrice) * this.openQuantity;
        this.profitRate = ((closePrice - this.openPrice) / this.openPrice) * 100;
    }

    public Trade(Member member, Strategy strategy, TradeOrderRequest order) {
        this.member = member;
        this.strategy = strategy;
        this.openPrice = order.getPrice();
        this.openQuantity = order.getQuantity();
        this.positionStatus = PositionStatus.OPEN;
    }

}
