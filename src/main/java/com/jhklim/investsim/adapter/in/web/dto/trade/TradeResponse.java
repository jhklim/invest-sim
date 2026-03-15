package com.jhklim.investsim.adapter.in.web.dto.trade;

import com.jhklim.investsim.domain.model.PositionStatus;
import com.jhklim.investsim.domain.model.Trade;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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
    private BigDecimal unrealizedProfit;
    private BigDecimal unrealizedProfitRate;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    public static TradeResponse from(Trade trade, BigDecimal currentPrice) {
        BigDecimal unrealizedProfit = null;
        BigDecimal unrealizedProfitRate = null;

        if (trade.getPositionStatus() == PositionStatus.OPEN && currentPrice != null) {
            BigDecimal openAmount = trade.getOpenPricePerShare().multiply(trade.getOpenQuantity());
            BigDecimal currentValue = currentPrice.multiply(trade.getOpenQuantity());
            unrealizedProfit = currentValue.subtract(openAmount);
            unrealizedProfitRate = unrealizedProfit
                    .divide(openAmount, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        return new TradeResponse(
                trade.getId(),
                trade.getOpenPricePerShare(),
                trade.getOpenQuantity(),
                trade.getCloseAmount(),
                trade.getProfitAmount(),
                trade.getProfitRate(),
                trade.getPositionStatus(),
                trade.getStrategy().getId(),
                trade.getStrategy().getName(),
                unrealizedProfit,
                unrealizedProfitRate,
                trade.getCreatedAt(),
                trade.getClosedAt()
        );
    }
}