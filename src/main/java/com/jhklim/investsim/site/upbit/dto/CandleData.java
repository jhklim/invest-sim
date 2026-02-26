package com.jhklim.investsim.site.upbit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CandleData {
    @JsonProperty("market")
    private String market;

    @JsonProperty("candle_date_time_kst")
    private String candleDateTimeKst;

    @JsonProperty("opening_price")
    private double openPrice;

    @JsonProperty("high_price")
    private double highPrice;

    @JsonProperty("low_price")
    private double lowPrice;

    @JsonProperty("trade_price")
    private double closePrice;  // 종가 = RSI 계산에 사용

    @JsonProperty("candle_acc_trade_volume")
    private double volume;
}
