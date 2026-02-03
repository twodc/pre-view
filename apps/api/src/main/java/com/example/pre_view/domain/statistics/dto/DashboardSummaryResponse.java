package com.example.pre_view.domain.statistics.dto;

/**
 * 대시보드 요약 정보 응답 DTO
 *
 * 사용자의 전체 면접 통계를 요약하여 제공합니다.
 */
public record DashboardSummaryResponse(
        int totalInterviews,
        int completedInterviews,
        int inProgressInterviews,
        Double averageScore,
        Double technicalAverageScore,
        Double personalityAverageScore
) {
    public static DashboardSummaryResponse of(
            int totalInterviews,
            int completedInterviews,
            int inProgressInterviews,
            Double averageScore,
            Double technicalAverageScore,
            Double personalityAverageScore
    ) {
        return new DashboardSummaryResponse(
                totalInterviews,
                completedInterviews,
                inProgressInterviews,
                averageScore,
                technicalAverageScore,
                personalityAverageScore
        );
    }
}
