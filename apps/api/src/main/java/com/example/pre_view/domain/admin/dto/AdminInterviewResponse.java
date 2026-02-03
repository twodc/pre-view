package com.example.pre_view.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;

/**
 * 관리자용 면접 정보 응답 DTO
 *
 * 관리자가 면접을 조회할 때 사용되는 상세 정보를 제공합니다.
 */
public record AdminInterviewResponse(
        Long id,
        Long memberId,
        String title,
        InterviewType type,
        String typeDescription,
        Position position,
        String positionDescription,
        ExperienceLevel level,
        String levelDescription,
        List<String> techStacks,
        InterviewStatus status,
        InterviewPhase currentPhase,
        String currentPhaseDescription,
        Integer totalQuestions,
        boolean hasResume,
        boolean hasPortfolio,
        boolean hasAiReport,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminInterviewResponse from(Interview interview) {
        return new AdminInterviewResponse(
                interview.getId(),
                interview.getMemberId(),
                interview.getTitle(),
                interview.getType(),
                interview.getType().getDescription(),
                interview.getPosition(),
                interview.getPosition().getDescription(),
                interview.getLevel(),
                interview.getLevel().getDescription(),
                interview.getTechStacks(),
                interview.getStatus(),
                interview.getCurrentPhase(),
                interview.getCurrentPhase() != null ? interview.getCurrentPhase().getDescription() : null,
                interview.getTotalQuestions(),
                interview.getResumeText() != null && !interview.getResumeText().isBlank(),
                interview.getPortfolioText() != null && !interview.getPortfolioText().isBlank(),
                interview.hasAiReport(),
                interview.getCreatedAt(),
                interview.getUpdatedAt()
        );
    }
}
