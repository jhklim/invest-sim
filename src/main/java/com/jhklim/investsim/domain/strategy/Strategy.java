package com.jhklim.investsim.domain.strategy;

import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.Trade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Strategy {

    @Id @GeneratedValue
    @Column(name = "strategy_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String name;
    private String description;

    private boolean isActive;

    @Enumerated(STRING)
    private Exchange exchange;

    private String market;

    @OneToOne(mappedBy = "strategy")
    private Trade trade;

    private Double buyAmount;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuyStrategy> buyStrategies = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SellStrategy> sellStrategies = new ArrayList<>();

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Strategy(Member member, Exchange exchange, String market, Double buyAmount) {
        this.member = member;
        this.exchange = exchange;
        this.market = market;
        this.buyAmount = buyAmount;
    }
}
