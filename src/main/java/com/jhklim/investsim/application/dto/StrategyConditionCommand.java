package com.jhklim.investsim.application.dto;

import com.jhklim.investsim.domain.model.Indicator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StrategyConditionCommand {
    private Indicator indicator;
    private double indicatorValue;
}