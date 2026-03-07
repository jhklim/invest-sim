package com.jhklim.investsim.adapter.out.persistence.jpa;

import com.jhklim.investsim.application.dto.ExchangeMarketSearchCond;
import com.jhklim.investsim.domain.model.Exchange;
import com.jhklim.investsim.domain.model.QStrategy;
import com.jhklim.investsim.domain.model.Strategy;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.jhklim.investsim.domain.model.QStrategy.*;
import static org.springframework.util.StringUtils.*;

@RequiredArgsConstructor
public class StrategyRepositoryImpl implements StrategyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Strategy> findActiveStrategiesByMarket(ExchangeMarketSearchCond condition) {
        return queryFactory
                .select(strategy)
                .from(strategy)
                .join(strategy.member).fetchJoin()
                .leftJoin(strategy.trade).fetchJoin()
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