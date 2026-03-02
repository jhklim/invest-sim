package com.jhklim.investsim.controller;

import com.jhklim.investsim.controller.dto.CreateStrategyRequest;
import com.jhklim.investsim.controller.dto.StrategyResponse;
import com.jhklim.investsim.service.StrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;

    @PostMapping
    public ResponseEntity<Void> create(@AuthenticationPrincipal Long memberId,
            @RequestBody CreateStrategyRequest request) {
        strategyService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<StrategyResponse>> getMyStrategies(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(strategyService.findByMember(memberId));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        strategyService.activate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        strategyService.deactivate(id);
        return ResponseEntity.ok().build();
    }
}