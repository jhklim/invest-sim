package com.jhklim.investsim.application.dto;

import com.jhklim.investsim.domain.model.Exchange;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CreateStrategyCommand {
    private String name;
    private String description;
    private Exchange exchange;
    private String market;
    private BigDecimal buyAmount;
    private List<StrategyConditionCommand> buyConditions;
    private List<StrategyConditionCommand> sellConditions;
}