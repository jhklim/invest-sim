package com.jhklim.investsim.controller.dto;

import com.jhklim.investsim.domain.strategy.Indicator;
import lombok.Getter;

@Getter
public class StrategyConditionRequest {
    private Indicator indicator;
    private double indicatorValue;
}