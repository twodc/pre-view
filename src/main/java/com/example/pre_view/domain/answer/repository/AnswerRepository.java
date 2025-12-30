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
}
