package com.jhklim.investsim.controller.dto.strategy;

import com.jhklim.investsim.domain.strategy.BuyStrategy;
import com.jhklim.investsim.domain.strategy.Indicator;
import com.jhklim.investsim.domain.strategy.SellStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StrategyConditionResponse {
    private Indicator indicator;
    private double indicatorValue;

    public static StrategyConditionResponse from(BuyStrategy b) {
        return new StrategyConditionResponse(b.getIndicator(), b.getIndicatorValue());
    }

    public static StrategyConditionResponse from(SellStrategy s) {
        return new StrategyConditionResponse(s.getIndicator(), s.getIndicatorValue());
    }
}