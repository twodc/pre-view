package com.example.pre_view.domain.member.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 회원을 조회합니다.
     */
    Optional<Member> findByEmail(String email);

    /**
     * 해당 이메일의 회원이 존재하는지 확인합니다.
     */
    boolean existsByEmail(String email);

    // ===== 관리자용 쿼리 =====

    /**
     * 전체 회원 목록 조회 (페이징, 활성화된 계정만)
     */
    Page<Member> findByDeletedFalse(Pageable pageable);

    /**
     * 전체 회원 목록 조회 (페이징, 모든 계정)
     */
    Page<Member> findAll(Pageable pageable);

    /**
     * 권한별 회원 수 조회
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.role = :role AND m.deleted = false")
    long countByRole(@Param("role") Role role);

    /**
     * 전체 활성 회원 수 조회
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.deleted = false")
    long countAllActive();

    /**
     * 기간별 가입 회원 수 조회
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.createdAt >= :startDate AND m.deleted = false")
    long countSince(@Param("startDate") LocalDateTime startDate);
}
