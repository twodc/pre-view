package com.example.pre_view.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pre_view.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 회원을 조회합니다.
     */
    Optional<Member> findByEmail(String email);

    /**
     * 해당 이메일의 회원이 존재하는지 확인합니다.
     */
    boolean existsByEmail(String email);
}
