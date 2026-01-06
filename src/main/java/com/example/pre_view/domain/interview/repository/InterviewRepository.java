package com.example.pre_view.domain.interview.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pre_view.domain.interview.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Page<Interview> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Optional<Interview> findByIdAndDeletedFalse(Long id);

    Page<Interview> findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Interview> findByIdAndMemberIdAndDeletedFalse(Long id, Long memberId);
}
