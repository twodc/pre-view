package com.example.pre_view.domain.admin.dto;

import java.util.List;

/**
 * 시스템 전체 통계 응답 DTO
 *
 * 관리자가 시스템 전체 현황을 파악할 수 있는 통계 정보를 제공합니다.
 */
public record SystemStatisticsResponse(
        // 회원 통계
        long totalMembers,
        long adminMembers,
        long userMembers,
        long newMembersToday,
        long newMembersThisWeek,
        long newMembersThisMonth,

        // 면접 통계
        long totalInterviews,
        long readyInterviews,
        long inProgressInterviews,
        long completedInterviews,
        long newInterviewsToday,
        long newInterviewsThisWeek,

        // 답변 통계
        long totalAnswers,
        Double overallAverageScore,

        // 일별 추이 (최근 7일)
        List<DailyCountResponse> dailyInterviewCounts,
        List<DailyCountResponse> dailyMemberCounts
) {
    public static SystemStatisticsResponse of(
            long totalMembers,
            long adminMembers,
            long userMembers,
            long newMembersToday,
            long newMembersThisWeek,
            long newMembersThisMonth,
            long totalInterviews,
            long readyInterviews,
            long inProgressInterviews,
            long completedInterviews,
            long newInterviewsToday,
            long newInterviewsThisWeek,
            long totalAnswers,
            Double overallAverageScore,
            List<DailyCountResponse> dailyInterviewCounts,
            List<DailyCountResponse> dailyMemberCounts
    ) {
        return new SystemStatisticsResponse(
                totalMembers,
                adminMembers,
                userMembers,
                newMembersToday,
                newMembersThisWeek,
                newMembersThisMonth,
                totalInterviews,
                readyInterviews,
                inProgressInterviews,
                completedInterviews,
                newInterviewsToday,
                newInterviewsThisWeek,
                totalAnswers,
                overallAverageScore,
                dailyInterviewCounts,
                dailyMemberCounts
        );
    }
}
