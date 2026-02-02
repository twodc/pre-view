package com.example.pre_view.domain.interview.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewStatus;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Page<Interview> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Optional<Interview> findByIdAndDeletedFalse(Long id);

    Page<Interview> findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Interview> findByIdAndMemberIdAndDeletedFalse(Long id, Long memberId);

    // ===== 사용자 통계 쿼리 =====

    /**
     * 회원의 전체 면접 수 조회
     */
    @Query("SELECT COUNT(i) FROM Interview i WHERE i.memberId = :memberId AND i.deleted = false")
    int countByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 상태별 면접 수 조회
     */
    @Query("SELECT COUNT(i) FROM Interview i WHERE i.memberId = :memberId AND i.status = :status AND i.deleted = false")
    int countByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") InterviewStatus status);

    /**
     * 회원의 최근 N개 면접 조회
     */
    @Query("SELECT i FROM Interview i WHERE i.memberId = :memberId AND i.deleted = false ORDER BY i.createdAt DESC")
    List<Interview> findRecentByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 회원의 월별 면접 수 조회 (점수 추이 계산용)
     */
    @Query("SELECT i FROM Interview i WHERE i.memberId = :memberId AND i.deleted = false AND i.status = 'DONE' " +
           "AND i.createdAt >= :startDate ORDER BY i.createdAt ASC")
    List<Interview> findCompletedByMemberIdSince(@Param("memberId") Long memberId, @Param("startDate") LocalDateTime startDate);

    // ===== 관리자 통계 쿼리 =====

    /**
     * 전체 면접 수 조회 (관리자용)
     */
    @Query("SELECT COUNT(i) FROM Interview i WHERE i.deleted = false")
    long countAllActive();

    /**
     * 상태별 전체 면접 수 조회 (관리자용)
     */
    @Query("SELECT COUNT(i) FROM Interview i WHERE i.status = :status AND i.deleted = false")
    long countByStatus(@Param("status") InterviewStatus status);

    /**
     * 기간별 면접 수 조회 (관리자용)
     */
    @Query("SELECT COUNT(i) FROM Interview i WHERE i.createdAt >= :startDate AND i.deleted = false")
    long countSince(@Param("startDate") LocalDateTime startDate);

    /**
     * 관리자용 면접 목록 필터링 조회
     */
    @Query("SELECT i FROM Interview i WHERE i.deleted = false " +
           "AND (:memberId IS NULL OR i.memberId = :memberId) " +
           "AND (:status IS NULL OR i.status = :status)")
    Page<Interview> findAllWithFilters(
            @Param("memberId") Long memberId,
            @Param("status") InterviewStatus status,
            Pageable pageable
    );
}
