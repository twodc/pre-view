package com.example.pre_view.domain.admin.dto;

import java.time.LocalDate;

/**
 * 일별 카운트 응답 DTO
 *
 * 일별 통계 데이터를 표현합니다.
 */
public record DailyCountResponse(
        LocalDate date,
        long count
) {
    public static DailyCountResponse of(LocalDate date, long count) {
        return new DailyCountResponse(date, count);
    }
}
