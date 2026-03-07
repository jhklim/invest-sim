package com.jhklim.investsim.application.dto;

import com.jhklim.investsim.domain.model.Exchange;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExchangeMarketSearchCond {
    private Exchange exchange;
    private String market;
}