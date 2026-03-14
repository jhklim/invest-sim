package com.jhklim.investsim.adapter.in.web.dto.member;

import com.jhklim.investsim.application.dto.MemberInfoResult;

import java.math.BigDecimal;

public record MemberResponse(
        String nickname,
        String email,
        BigDecimal totalAsset,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal reservedAmount,
        BigDecimal openPositionValue
) {
    public static MemberResponse from(MemberInfoResult result) {
        return new MemberResponse(
                result.nickname(),
                result.email(),
                result.totalAsset(),
                result.balance(),
                result.availableBalance(),
                result.reservedAmount(),
                result.openPositionValue()
        );
    }
}