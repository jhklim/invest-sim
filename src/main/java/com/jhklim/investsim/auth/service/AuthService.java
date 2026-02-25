package com.jhklim.investsim.auth.service;

import com.jhklim.investsim.auth.dto.LoginRequest;
import com.jhklim.investsim.auth.dto.SignupRequest;
import com.jhklim.investsim.auth.jwt.JwtTokenProvider;
import com.jhklim.investsim.domain.Member;
import com.jhklim.investsim.domain.Role;
import com.jhklim.investsim.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        Member member = new Member(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Role.USER,
                request.getNickname()
        );

        memberRepository.save(member);
    }

    public String login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일에 대한 유저가 존재하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtTokenProvider.createToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
    }
}
