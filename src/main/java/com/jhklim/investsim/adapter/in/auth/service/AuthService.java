package com.jhklim.investsim.adapter.in.auth.service;

import com.jhklim.investsim.adapter.in.auth.dto.LoginRequest;
import com.jhklim.investsim.adapter.in.auth.dto.LoginResponse;
import com.jhklim.investsim.adapter.in.auth.dto.SignupRequest;
import com.jhklim.investsim.adapter.in.auth.jwt.JwtTokenProvider;
import com.jhklim.investsim.adapter.out.redis.RefreshTokenStore;
import com.jhklim.investsim.application.port.out.MemberPort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import com.jhklim.investsim.domain.model.Member;
import com.jhklim.investsim.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final MemberPort memberPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public void signup(SignupRequest request) {
        if (memberPort.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = new Member(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Role.USER,
                request.getNickname()
        );

        memberPort.save(member);
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberPort.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );

        String refreshToken = UUID.randomUUID().toString();
        refreshTokenStore.save(member.getId(), refreshToken, REFRESH_TOKEN_TTL);

        return new LoginResponse(accessToken, refreshToken);
    }

    public String refresh(Long memberId, String refreshToken) {
        String stored = refreshTokenStore.find(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return jwtTokenProvider.createToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
    }

    public void logout(Long memberId) {
        refreshTokenStore.delete(memberId);
    }
}