package com.jhklim.investsim.controller.dto.trade;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;


@Getter
public class CreateTradeRequest {

    @NotNull
    private Long strategyId;
}
