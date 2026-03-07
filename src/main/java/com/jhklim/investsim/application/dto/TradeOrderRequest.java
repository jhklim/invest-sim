package com.jhklim.investsim.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TradeOrderRequest {
    private BigDecimal price;
    private BigDecimal quantity;

    public BigDecimal getTotalOrderPrice() {
        return this.price.multiply(this.quantity);
    }
}