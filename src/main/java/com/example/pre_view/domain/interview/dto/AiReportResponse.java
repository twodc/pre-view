package com.example.pre_view.domain.interview.dto;

import java.util.List;

public record AiReportResponse(
    String summary,
    List<String> strengths,
    List<String> improvements,
    List<String> recommendedTopics,
    Integer overallScore,
    List<QuestionFeedback> questionFeedbacks
) {
    public record QuestionFeedback(
        String question,
        String userAnswer,
        String detailedFeedback,
        String recommendedAnswer,
        Integer score
    ) {}
}
