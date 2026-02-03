package com.example.pre_view.domain.statistics.dto;

import java.time.LocalDate;

/**
 * 점수 추이 응답 DTO
 *
 * 월별/주별 평균 점수 추이를 제공합니다.
 */
public record ScoreTrendResponse(
        LocalDate date,
        String period,
        Double averageScore,
        int interviewCount
) {
    public static ScoreTrendResponse of(
            LocalDate date,
            String period,
            Double averageScore,
            int interviewCount
    ) {
        return new ScoreTrendResponse(date, period, averageScore, interviewCount);
    }
}
