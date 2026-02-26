package com.jhklim.investsim.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TradeOrderRequest {
    private double price;
    private double quantity;

    public double getTotalOrderPrice() {
        return this.getPrice() * getQuantity();
    }
}
