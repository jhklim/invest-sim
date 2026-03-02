package com.jhklim.investsim.site.upbit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CandleData {
    @JsonProperty("market")
    private String market;

    @JsonProperty("candle_date_time_kst")
    private String candleDateTimeKst;

    @JsonProperty("opening_price")
    private BigDecimal openPrice;

    @JsonProperty("high_price")
    private BigDecimal highPrice;

    @JsonProperty("low_price")
    private BigDecimal lowPrice;

    @JsonProperty("trade_price")
    private BigDecimal closePrice;  // 종가 = RSI 계산에 사용

    @JsonProperty("candle_acc_trade_volume")
    private BigDecimal volume;
}