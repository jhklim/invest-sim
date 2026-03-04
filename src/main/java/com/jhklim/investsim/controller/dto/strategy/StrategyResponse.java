package com.jhklim.investsim.controller.dto.strategy;

import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.strategy.Strategy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class StrategyResponse {
    private Long id;
    private String name;
    private String description;
    private Exchange exchange;
    private String market;
    private BigDecimal buyAmount;
    private boolean active;
    private List<StrategyConditionResponse> buyConditions;
    private List<StrategyConditionResponse> sellConditions;

    public static StrategyResponse from(Strategy strategy) {
        return new StrategyResponse(
                strategy.getId(),
                strategy.getName(),
                strategy.getDescription(),
                strategy.getExchange(),
                strategy.getMarket(),
                strategy.getBuyAmount(),
                strategy.isActive(),
                strategy.getBuyStrategies().stream().map(StrategyConditionResponse::from).toList(),
                strategy.getSellStrategies().stream().map(StrategyConditionResponse::from).toList()
        );
    }
}
