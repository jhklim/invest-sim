package com.jhklim.investsim.repository;

import com.jhklim.investsim.domain.Exchange;
import com.jhklim.investsim.domain.strategy.QBuyStrategy;
import com.jhklim.investsim.domain.strategy.QSellStrategy;
import com.jhklim.investsim.domain.strategy.QStrategy;
import com.jhklim.investsim.domain.strategy.Strategy;
import com.jhklim.investsim.dto.ExchangeMarketSearchCond;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.jhklim.investsim.domain.strategy.QBuyStrategy.*;
import static com.jhklim.investsim.domain.strategy.QSellStrategy.*;
import static com.jhklim.investsim.domain.strategy.QStrategy.*;
import static org.springframework.util.StringUtils.*;

@RequiredArgsConstructor
public class StrategyRepositoryImpl implements StrategyRepositoryCustom {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        return queryFactory
                .select(strategy)
                .from(strategy)
                .where(
                        strategy.isActive.eq(true),
                        exchangeEq(condition.getExchange()),
                        marketEq(condition.getMarket())
                )
                .fetch();
    }

    private BooleanExpression exchangeEq(Exchange exchange) {
        return exchange != null ? strategy.exchange.eq(exchange) : null;
    }

    private BooleanExpression marketEq(String market) {
        return hasText(market) ? strategy.market.eq(market) : null;
    }

}
