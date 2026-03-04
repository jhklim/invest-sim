package com.jhklim.investsim.controller.dto.strategy;

import com.jhklim.investsim.domain.Exchange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class CreateStrategyRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Exchange exchange;

    @NotBlank
    private String market;

    @NotNull
    @Positive
    private BigDecimal buyAmount;

    @NotEmpty
    @Valid
    private List<StrategyConditionRequest> buyConditions;

    @NotEmpty
    @Valid
    private List<StrategyConditionRequest> sellConditions;
}