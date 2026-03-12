package com.jhklim.investsim.adapter.in.auth.service;

import com.jhklim.investsim.adapter.in.auth.dto.LoginRequest;
import com.jhklim.investsim.adapter.in.auth.dto.SignupRequest;
import com.jhklim.investsim.adapter.in.auth.jwt.JwtTokenProvider;
import com.jhklim.investsim.application.port.out.MemberPort;
import com.jhklim.investsim.common.exception.BusinessException;
import com.jhklim.investsim.common.exception.ErrorCode;
import com.jhklim.investsim.domain.model.Member;
import com.jhklim.investsim.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberPort memberPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

    public String login(LoginRequest request) {
        Member member = memberPort.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return jwtTokenProvider.createToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
    }
}