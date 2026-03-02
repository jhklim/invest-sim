package com.jhklim.investsim.controller.dto;

import com.jhklim.investsim.domain.Exchange;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class CreateStrategyRequest {
    private String name;
    private String description;
    private Exchange exchange;
    private String market;
    private BigDecimal buyAmount;
    private List<StrategyConditionRequest> buyConditions;
    private List<StrategyConditionRequest> sellConditions;
}