package com.example.pre_view.domain.answer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.question q " +
           "WHERE q.interview.id = :interviewId " +
           "ORDER BY q.sequence ASC, a.createdAt ASC")
    List<Answer> findByInterviewIdWithQuestion(@Param("interviewId") Long interviewId);

    /**
     * 면접 ID와 단계(phase)로 답변 조회 (쿼리 최적화)
     * 전체 답변을 조회 후 필터링하는 대신 DB 레벨에서 필터링
     */
    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.question q " +
           "WHERE q.interview.id = :interviewId AND q.phase = :phase " +
           "ORDER BY q.sequence ASC, a.createdAt ASC")
    List<Answer> findByInterviewIdAndPhase(
            @Param("interviewId") Long interviewId,
            @Param("phase") InterviewPhase phase
    );

    // ===== 사용자 통계 쿼리 =====

    /**
     * 회원의 전체 평균 점수 계산
     */
    @Query("SELECT AVG(a.score) FROM Answer a " +
           "JOIN a.question q " +
           "JOIN Interview i ON q.interview.id = i.id " +
           "WHERE i.memberId = :memberId AND i.deleted = false AND a.score IS NOT NULL")
    Double findAverageScoreByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 단계별 평균 점수 계산
     */
    @Query("SELECT AVG(a.score) FROM Answer a " +
           "JOIN a.question q " +
           "JOIN Interview i ON q.interview.id = i.id " +
           "WHERE i.memberId = :memberId AND i.deleted = false AND q.phase = :phase AND a.score IS NOT NULL")
    Double findAverageScoreByMemberIdAndPhase(@Param("memberId") Long memberId, @Param("phase") InterviewPhase phase);

    /**
     * 회원의 단계별 답변 수 조회
     */
    @Query("SELECT COUNT(a) FROM Answer a " +
           "JOIN a.question q " +
           "JOIN Interview i ON q.interview.id = i.id " +
           "WHERE i.memberId = :memberId AND i.deleted = false AND q.phase = :phase")
    int countByMemberIdAndPhase(@Param("memberId") Long memberId, @Param("phase") InterviewPhase phase);

    /**
     * 특정 면접의 평균 점수 계산
     */
    @Query("SELECT AVG(a.score) FROM Answer a " +
           "JOIN a.question q " +
           "WHERE q.interview.id = :interviewId AND a.score IS NOT NULL")
    Double findAverageScoreByInterviewId(@Param("interviewId") Long interviewId);

    // ===== 관리자 통계 쿼리 =====

    /**
     * 전체 평균 점수 계산 (관리자용)
     */
    @Query("SELECT AVG(a.score) FROM Answer a " +
           "JOIN a.question q " +
           "JOIN Interview i ON q.interview.id = i.id " +
           "WHERE i.deleted = false AND a.score IS NOT NULL")
    Double findOverallAverageScore();

    /**
     * 전체 답변 수 조회 (관리자용)
     */
    @Query("SELECT COUNT(a) FROM Answer a " +
           "JOIN a.question q " +
           "JOIN Interview i ON q.interview.id = i.id " +
           "WHERE i.deleted = false")
    long countAllActive();
}
