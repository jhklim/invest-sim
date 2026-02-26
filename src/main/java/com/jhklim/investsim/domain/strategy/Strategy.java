package com.jhklim.investsim.domain.strategy;

import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.Trade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @OneToOne(mappedBy = "strategy")
    private Trade trade;

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuyStrategy> buyStrategies;

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SellStrategy> sellStrategies;
}
