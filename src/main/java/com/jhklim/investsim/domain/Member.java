package com.jhklim.investsim.domain;

import com.jhklim.investsim.domain.strategy.Strategy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String nickname;

    @OneToMany(mappedBy = "member")
    private List<Trade> trades = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Strategy> strategies = new ArrayList<>();

    @Version
    private int version; // 낙관적 락

    @Column(precision = 30, scale = 8)
    private BigDecimal balance;

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void deductBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔고 부족");
        }
        this.balance = this.balance.subtract(amount);
    }

    public Member(String email, String password, Role role, String nickname) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.nickname = nickname;
        this.balance = BigDecimal.ZERO;
    }

    public Member(String email, String password, Role role, String nickname, BigDecimal balance) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.nickname = nickname;
        this.balance = balance;
    }
}