package com.jhklim.investsim.adapter.in.web;

import com.jhklim.investsim.adapter.in.web.dto.trade.TradeResponse;
import com.jhklim.investsim.application.port.in.TradeUseCase;
import com.jhklim.investsim.application.port.out.CurrentPricePort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeUseCase tradeUseCase;
    private final CurrentPricePort currentPricePort;

    @GetMapping
    public ResponseEntity<List<TradeResponse>> getMyTrades(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                tradeUseCase.findByMember(memberId).stream()
                        .map(trade -> {
                            String market = trade.getStrategy().getMarket();
                            BigDecimal currentPrice = currentPricePort.exists(market)
                                    ? currentPricePort.get(market)
                                    : null;
                            return TradeResponse.from(trade, currentPrice);
                        })
                        .toList()
        );
    }
}