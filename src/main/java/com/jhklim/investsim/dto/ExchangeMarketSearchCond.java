package com.jhklim.investsim.dto;

import com.jhklim.investsim.domain.Exchange;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExchangeMarketSearchCond {
    private Exchange exchange;
    private String market;
}
