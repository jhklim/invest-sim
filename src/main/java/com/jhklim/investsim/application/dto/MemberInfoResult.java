package com.jhklim.investsim.application.dto;

import java.math.BigDecimal;

public record MemberInfoResult(
        String nickname,
        String email,
        BigDecimal totalAsset,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal reservedAmount,
        BigDecimal openPositionValue
) {}