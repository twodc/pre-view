package com.example.pre_view.domain.statistics.dto;

import com.example.pre_view.domain.interview.enums.InterviewPhase;

/**
 * 단계별 성과 응답 DTO
 *
 * 면접 단계별(TECHNICAL/PERSONALITY) 평균 점수와 답변 수를 제공합니다.
 */
public record PhasePerformanceResponse(
        InterviewPhase phase,
        String phaseDescription,
        Double averageScore,
        int totalAnswers
) {
    public static PhasePerformanceResponse of(
            InterviewPhase phase,
            Double averageScore,
            int totalAnswers
    ) {
        return new PhasePerformanceResponse(
                phase,
                phase != null ? phase.getDescription() : null,
                averageScore,
                totalAnswers
        );
    }
}
