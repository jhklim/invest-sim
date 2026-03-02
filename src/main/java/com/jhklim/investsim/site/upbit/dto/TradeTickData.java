package com.jhklim.investsim.site.upbit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TradeTickData {
    @JsonProperty("type")
    private String type;

    @JsonProperty("code")
    private String market;

    @JsonProperty("trade_price")
    private BigDecimal tradePrice;

    @JsonProperty("trade_volume")
    private BigDecimal tradeVolume;

    @JsonProperty("ask_bid")
    private String askBid; // BID(buy) / ASK(sell)

    @JsonProperty("timestamp")
    private long timestamp;
}