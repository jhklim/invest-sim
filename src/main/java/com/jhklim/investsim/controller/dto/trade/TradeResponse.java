package com.jhklim.investsim.controller.dto.trade;

import com.jhklim.investsim.domain.PositionStatus;
import com.jhklim.investsim.domain.Trade;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TradeResponse {
    private Long id;
    private BigDecimal openPricePerShare;
    private BigDecimal openQuantity;
    private BigDecimal closeAmount;
    private BigDecimal profitAmount;
    private BigDecimal profitRate;
    private PositionStatus positionStatus;
    private Long strategyId;
    private String strategyName;

    public static TradeResponse from(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getOpenPricePerShare(),
                trade.getOpenQuantity(),
                trade.getCloseAmount(),
                trade.getProfitAmount(),
                trade.getProfitRate(),
                trade.getPositionStatus(),
                trade.getStrategy().getId(),
                trade.getStrategy().getName()
        );
    }
}
