package com.example.pre_view.domain.answer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.answer.entity.Answer;

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
            @Param("phase") com.example.pre_view.domain.interview.enums.InterviewPhase phase
    );
}
