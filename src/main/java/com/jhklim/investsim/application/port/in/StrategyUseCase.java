package com.jhklim.investsim.application.port.in;

import com.jhklim.investsim.application.dto.CreateStrategyCommand;
import com.jhklim.investsim.domain.model.Strategy;

import java.util.List;

public interface StrategyUseCase {
    List<Strategy> findByMember(Long memberId);
    void create(Long memberId, CreateStrategyCommand command);
    void activate(Long memberId, Long strategyId);
    void deactivate(Long memberId, Long strategyId);
    void delete(Long memberId, Long strategyId);
    void deactivateAfterBuy(Long strategyId);   // isActive=false, 캐시 유지 (매도 감시 계속)
    void autoDeactivateAfterSell(Long strategyId); // 캐시 제거 (완전 종료)
}