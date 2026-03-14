package com.jhklim.investsim.adapter.in.auth.controller;

import com.jhklim.investsim.adapter.in.auth.dto.LoginRequest;
import com.jhklim.investsim.adapter.in.auth.dto.LoginResponse;
import com.jhklim.investsim.adapter.in.auth.dto.RefreshRequest;
import com.jhklim.investsim.adapter.in.auth.dto.SignupRequest;
import com.jhklim.investsim.adapter.in.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@Valid @RequestBody RefreshRequest request) {
        String newAccessToken = authService.refresh(request.getMemberId(), request.getRefreshToken());
        return ResponseEntity.ok(newAccessToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String bearerToken) {
        authService.logout(bearerToken.substring(7));
        return ResponseEntity.noContent().build();
    }
}