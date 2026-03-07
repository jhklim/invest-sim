package com.jhklim.investsim.config.init;

import com.jhklim.investsim.application.port.out.MemberPort;
import com.jhklim.investsim.domain.model.Member;
import com.jhklim.investsim.domain.model.Role;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final MemberPort memberPort;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    private void initAdmin() {
        if (!memberPort.existsByEmail("admin@gmail.com")) {

            Member admin = new Member(
                    "admin@gmail.com",
                    passwordEncoder.encode("admin123"),
                    Role.ADMIN,
                    "admin"
            );

            memberPort.save(admin);
        }
    }
}