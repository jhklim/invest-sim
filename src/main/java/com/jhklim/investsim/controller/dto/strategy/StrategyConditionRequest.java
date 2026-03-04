package com.jhklim.investsim.controller.dto.strategy;

import com.jhklim.investsim.domain.strategy.Indicator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class StrategyConditionRequest {
    @NotNull
    private Indicator indicator;

    @Positive
    private double indicatorValue;
}