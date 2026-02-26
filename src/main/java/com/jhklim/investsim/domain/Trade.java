package com.jhklim.investsim.domain;

import com.jhklim.investsim.domain.strategy.Strategy;
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

    private String symbol;

    private double openPrice;
    private double openQuantity;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @Enumerated(STRING)
    private PositionStatus positionStatus; // [OPEN, CLOSE]


    public double getTotalOpenPrice() {
        return this.getOpenPrice() * getOpenQuantity();
    }

}
