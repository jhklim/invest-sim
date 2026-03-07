package com.jhklim.investsim.adapter.in.web;

import com.jhklim.investsim.adapter.in.web.dto.trade.TradeResponse;
import com.jhklim.investsim.application.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    public ResponseEntity<List<TradeResponse>> getMyTrades(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(tradeService.findByMember(memberId));
    }
}