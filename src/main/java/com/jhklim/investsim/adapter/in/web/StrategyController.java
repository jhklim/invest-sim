package com.jhklim.investsim.adapter.in.web;

import com.jhklim.investsim.adapter.in.web.dto.strategy.CreateStrategyRequest;
import com.jhklim.investsim.adapter.in.web.dto.strategy.StrategyResponse;
import com.jhklim.investsim.application.dto.CreateStrategyCommand;
import com.jhklim.investsim.application.dto.StrategyConditionCommand;
import com.jhklim.investsim.application.port.in.StrategyUseCase;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyUseCase strategyUseCase;

    @PostMapping
    public ResponseEntity<Void> create(@AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateStrategyRequest request) {
        CreateStrategyCommand command = new CreateStrategyCommand(
                request.getName(),
                request.getDescription(),
                request.getExchange(),
                request.getMarket(),
                request.getBuyAmount(),
                request.getBuyConditions().stream()
                        .map(c -> new StrategyConditionCommand(c.getIndicator(), c.getIndicatorValue()))
                        .toList(),
                request.getSellConditions().stream()
                        .map(c -> new StrategyConditionCommand(c.getIndicator(), c.getIndicatorValue()))
                        .toList()
        );
        strategyUseCase.create(memberId, command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<StrategyResponse>> getMyStrategies(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                strategyUseCase.findByMember(memberId).stream()
                        .map(StrategyResponse::from)
                        .toList()
        );
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@AuthenticationPrincipal Long memberId,
                                         @PathVariable Long id) {
        strategyUseCase.activate(memberId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal Long memberId,
                                           @PathVariable Long id) {
        strategyUseCase.deactivate(memberId, id);
        return ResponseEntity.ok().build();
    }
}