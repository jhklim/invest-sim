package com.jhklim.investsim.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Strategy {

    @Id @GeneratedValue
    @Column(name = "strategy_id")
    private Long id;

    private String name;
    private String description;

    @OneToOne(mappedBy = "strategy")
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
