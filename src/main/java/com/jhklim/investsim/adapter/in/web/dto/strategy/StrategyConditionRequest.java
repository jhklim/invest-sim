package com.jhklim.investsim.adapter.in.web.dto.strategy;

import com.jhklim.investsim.domain.model.Indicator;
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