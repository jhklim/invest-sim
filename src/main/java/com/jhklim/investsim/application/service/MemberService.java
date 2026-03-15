package com.jhklim.investsim.application.service;

import com.jhklim.investsim.application.dto.MemberInfoResult;
import com.jhklim.investsim.application.port.in.MemberUseCase;
import com.jhklim.investsim.application.port.out.CurrentPricePort;
import com.jhklim.investsim.application.port.out.MemberPort;
import com.jhklim.investsim.application.port.out.StrategyPort;
import com.jhklim.investsim.application.port.out.TradePort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import com.jhklim.investsim.domain.model.Member;
import com.jhklim.investsim.domain.model.PositionStatus;
import com.jhklim.investsim.domain.model.Strategy;
import com.jhklim.investsim.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {

    private final MemberPort memberPort;
    private final StrategyPort strategyPort;
    private final TradePort tradePort;
    private final CurrentPricePort currentPricePort;

    @Override
    public MemberInfoResult getMyInfo(Long memberId) {
        Member member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // isActive=true 전략 = 매수 대기 중, buyAmount가 잔고에서 차감된 상태
        BigDecimal reservedAmount = strategyPort.findByMemberId(memberId).stream()
                .filter(Strategy::isActive)
                .map(Strategy::getBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // OPEN 포지션 현재 평가액 합산
        BigDecimal openPositionValue = tradePort.findByMemberId(memberId).stream()
                .filter(t -> t.getPositionStatus() == PositionStatus.OPEN)
                .map(t -> {
                    String market = t.getStrategy().getMarket();
                    BigDecimal currentPrice = currentPricePort.exists(market)
                            ? currentPricePort.get(market)
                            : t.getOpenPricePerShare(); // 현재가 없으면 매수가로 대체
                    return t.getOpenQuantity().multiply(currentPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableBalance = member.getBalance();
        BigDecimal balance = availableBalance.add(reservedAmount);
        BigDecimal totalAsset = balance.add(openPositionValue);

        return new MemberInfoResult(
                member.getNickname(),
                member.getEmail(),
                totalAsset,
                balance,
                availableBalance,
                reservedAmount,
                openPositionValue
        );
    }
}