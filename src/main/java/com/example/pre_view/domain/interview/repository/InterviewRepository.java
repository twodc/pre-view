package com.example.pre_view.domain.interview.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.interview.entity.Interview;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    
    // 삭제되지 않은 면접 목록 조회 (페이징 적용)
    @Query("SELECT i FROM Interview i WHERE i.deleted = false ORDER BY i.createdAt DESC")
    Page<Interview> findAllActive(Pageable pageable);
    
    // 삭제되지 않은 특정 면접 조회
    @Query("SELECT i FROM Interview i WHERE i.id = :id AND i.deleted = false")
    java.util.Optional<Interview> findByIdAndNotDeleted(@Param("id") Long id);
}
