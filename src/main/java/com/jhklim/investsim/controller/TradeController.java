package com.jhklim.investsim.controller;

import com.jhklim.investsim.controller.dto.trade.CreateTradeRequest;
import com.jhklim.investsim.controller.dto.trade.TradeResponse;
import com.jhklim.investsim.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<Void> create(@AuthenticationPrincipal Long memberId,
                                       @Valid @RequestBody CreateTradeRequest request) {
        tradeService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<TradeResponse>> getMyTrades(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(tradeService.findByMember(memberId));
    }

}
