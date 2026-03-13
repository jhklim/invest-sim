package com.jhklim.investsim.adapter.in.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotNull
    private Long memberId;

    @NotBlank
    private String refreshToken;
}