package com.jhklim.investsim.domain.strategy;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuyStrategy {

    @Id @GeneratedValue
    @Column(name = "buy_Strategy_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @Enumerated(EnumType.STRING)
    private Indicator indicator;

    private double Value;
}
