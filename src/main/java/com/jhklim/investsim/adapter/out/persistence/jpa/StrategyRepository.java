package com.jhklim.investsim.adapter.out.persistence.jpa;

import com.jhklim.investsim.domain.model.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StrategyRepository extends JpaRepository<Strategy, Long>, StrategyRepositoryCustom {
    List<Strategy> findByMemberId(Long memberId);
    List<Strategy> findAllByIsActiveTrue();
    long countByMemberId(Long memberId);
}