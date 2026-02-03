package com.example.pre_view.domain.question.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.question.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {

       @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "parentQuestion" })
       @Query("SELECT q FROM Question q " +
                     "LEFT JOIN FETCH q.parentQuestion " +
                     "WHERE q.interview.id = :interviewId ORDER BY q.sequence ASC")
       List<Question> findByInterviewIdOrderBySequence(@Param("interviewId") Long interviewId);

       @Query("SELECT q FROM Question q WHERE q.parentQuestion.id = :parentQuestionId")
       List<Question> findByParentQuestionId(@Param("parentQuestionId") Long parentQuestionId);

       @Query("SELECT q FROM Question q JOIN FETCH q.interview WHERE q.id = :id")
       Optional<Question> findByIdWithInterview(@Param("id") Long id);

       // size()만 필요한 경우
       @Query("SELECT COUNT(q) FROM Question q WHERE q.interview.id = :interviewId")
       int countByInterviewId(@Param("interviewId") Long interviewId);

       // 특정 단계의 질문 목록 조회
       @Query("SELECT q FROM Question q " +
                     "LEFT JOIN FETCH q.parentQuestion " +
                     "WHERE q.interview.id = :interviewId AND q.phase = :phase " +
                     "ORDER BY q.sequence ASC")
       List<Question> findByInterviewIdAndPhaseOrderBySequence(
                     @Param("interviewId") Long interviewId,
                     @Param("phase") InterviewPhase phase);
}
