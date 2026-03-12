package com.jhklim.investsim.adapter.out.upbit;

import com.jhklim.investsim.application.port.out.CurrentPricePort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrentPriceStore implements CurrentPricePort {

    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

    public void update(String market, BigDecimal price) {
        prices.put(market, price);
    }

    public BigDecimal get(String market) {
        if (!prices.containsKey(market)) {
            throw new BusinessException(ErrorCode.CURRENT_PRICE_NOT_FOUND);
        }
        return prices.get(market);
    }

    public boolean exists(String market) {
        return prices.containsKey(market);
    }
}