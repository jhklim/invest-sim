package com.jhklim.investsim.application.port.out;

import com.jhklim.investsim.domain.model.Member;

import java.util.Optional;

public interface MemberPort {
    Member save(Member member);
    Optional<Member> findById(Long id);
    Boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
    long count();
}