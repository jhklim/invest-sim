package com.jhklim.investsim.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Strategy extends BaseTimeEntity {

    @Id @GeneratedValue
    @Column(name = "strategy_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String name;
    private String description;

    private boolean isActive;

    private LocalDateTime deletedAt;

    @Enumerated(STRING)
    private Exchange exchange;

    private String market;

    @Column(precision = 30, scale = 8)
    private BigDecimal buyAmount;

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

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public Strategy(Member member, Exchange exchange, String market, BigDecimal buyAmount) {
        this.member = member;
        this.exchange = exchange;
        this.market = market;
        this.buyAmount = buyAmount;
    }

    public Strategy(Member member, String name, String description, Exchange exchange, String market, BigDecimal buyAmount) {
        this.member = member;
        this.name = name;
        this.description = description;
        this.exchange = exchange;
        this.market = market;
        this.buyAmount = buyAmount;
    }
}