package com.jhklim.investsim.repository;

import com.jhklim.investsim.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
