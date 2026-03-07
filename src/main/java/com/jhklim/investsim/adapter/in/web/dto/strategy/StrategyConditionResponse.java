package com.jhklim.investsim.adapter.in.web.dto.strategy;

import com.jhklim.investsim.domain.model.BuyStrategy;
import com.jhklim.investsim.domain.model.Indicator;
import com.jhklim.investsim.domain.model.SellStrategy;
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