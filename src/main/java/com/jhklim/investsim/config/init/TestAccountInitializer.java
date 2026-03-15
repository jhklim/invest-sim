package com.jhklim.investsim.config.init;

import com.jhklim.investsim.application.port.out.MemberPort;
import com.jhklim.investsim.domain.model.Member;
import com.jhklim.investsim.domain.model.Role;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TestAccountInitializer {

    private final MemberPort memberPort;
    private final PasswordEncoder passwordEncoder;

    @Value("${test-account.email}")
    private String email;

    @Value("${test-account.password}")
    private String password;

    @PostConstruct
    private void initTestAccount() {
        if (!memberPort.existsByEmail(email)) {
            Member testUser = new Member(
                    email,
                    passwordEncoder.encode(password),
                    Role.USER,
                    "tester",
                    new BigDecimal("100000")
            );
            memberPort.save(testUser);
        }
    }
}