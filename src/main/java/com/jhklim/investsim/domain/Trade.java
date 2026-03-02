package com.jhklim.investsim.domain;

import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.TradeOrderRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    @Column(precision = 30, scale = 8)
    private BigDecimal openPrice;

    @Column(precision = 30, scale = 8)
    private BigDecimal openQuantity;

    @Column(precision = 30, scale = 8)
    private BigDecimal closePrice;

    @Column(precision = 30, scale = 8)
    private BigDecimal profitAmount;

    @Column(precision = 10, scale = 4)
    private BigDecimal profitRate;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @Enumerated(STRING)
    private PositionStatus positionStatus; // [OPEN, CLOSE]

    public BigDecimal getTotalOpenPrice() {
        return this.openPrice.multiply(this.openQuantity);
    }

    public void close(BigDecimal closePrice) {
        this.positionStatus = PositionStatus.CLOSE;
        this.closePrice = closePrice;
        this.profitAmount = closePrice.subtract(this.openPrice).multiply(this.openQuantity);
        this.profitRate = closePrice.subtract(this.openPrice)
                .divide(this.openPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    public Trade(Member member, Strategy strategy, TradeOrderRequest order) {
        this.member = member;
        this.strategy = strategy;
        this.openPrice = order.getPrice();
        this.openQuantity = order.getQuantity();
        this.positionStatus = PositionStatus.OPEN;
    }
}