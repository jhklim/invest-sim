package com.jhklim.investsim.adapter.in.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LogoutRequest {

    @NotNull
    private Long memberId;
}