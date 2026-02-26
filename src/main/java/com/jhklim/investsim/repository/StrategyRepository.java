package com.jhklim.investsim.repository;

import com.jhklim.investsim.domain.strategy.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyRepository extends JpaRepository<Strategy, Long>, StrategyRepositoryCustom {
}
