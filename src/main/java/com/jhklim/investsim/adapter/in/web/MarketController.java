package com.jhklim.investsim.adapter.in.web;

import com.jhklim.investsim.application.port.out.CurrentPricePort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final CurrentPricePort currentPricePort;

    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> getPrice(@RequestParam String market) {
        if (!currentPricePort.exists(market)) {
            throw new BusinessException(ErrorCode.CURRENT_PRICE_NOT_FOUND);
        }
        BigDecimal price = currentPricePort.get(market);
        return ResponseEntity.ok(Map.of("market", market, "price", price));
    }
}