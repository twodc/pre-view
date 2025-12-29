package com.example.pre_view.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.question.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q WHERE q.interview.id = :interviewId")
    List<Question> findByInterviewId(@Param("interviewId") Long interviewId);

    @Query("SELECT q FROM Question q WHERE q.interview.id = :interviewId ORDER BY q.sequence ASC")
    List<Question> findByInterviewIdOrderBySequence(@Param("interviewId") Long interviewId);
    
    @Query("SELECT q FROM Question q WHERE q.parentQuestion.id = :parentQuestionId")
    List<Question> findByParentQuestionId(@Param("parentQuestionId") Long parentQuestionId);
    
    @Query("SELECT q FROM Question q WHERE q.interview.id = :interviewId AND q.isAnswered = false ORDER BY q.sequence ASC")
    List<Question> findUnansweredQuestionsByInterviewId(@Param("interviewId") Long interviewId);
}
