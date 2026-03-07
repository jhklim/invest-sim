package com.jhklim.investsim.application.port.out;

import java.math.BigDecimal;

public interface CurrentPricePort {
    void update(String market, BigDecimal price);
    BigDecimal get(String market);
    boolean exists(String market);
}