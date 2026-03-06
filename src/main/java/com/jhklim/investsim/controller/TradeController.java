package com.jhklim.investsim.controller;

import com.jhklim.investsim.controller.dto.trade.TradeResponse;
import com.jhklim.investsim.service.TradeService;
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
