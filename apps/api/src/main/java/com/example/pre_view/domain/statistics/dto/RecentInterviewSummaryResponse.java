package com.example.pre_view.domain.statistics.dto;

import java.time.LocalDateTime;

import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;

/**
 * 최근 면접 요약 응답 DTO
 *
 * 최근 면접의 간략한 정보와 점수를 제공합니다.
 */
public record RecentInterviewSummaryResponse(
        Long id,
        String title,
        InterviewType type,
        String typeDescription,
        Position position,
        String positionDescription,
        InterviewStatus status,
        Double averageScore,
        LocalDateTime createdAt
) {
    public static RecentInterviewSummaryResponse of(Interview interview, Double averageScore) {
        return new RecentInterviewSummaryResponse(
                interview.getId(),
                interview.getTitle(),
                interview.getType(),
                interview.getType().getDescription(),
                interview.getPosition(),
                interview.getPosition().getDescription(),
                interview.getStatus(),
                averageScore,
                interview.getCreatedAt()
        );
    }
}
