package com.example.pre_view.domain.llm.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM 리포트 응답 DTO
 */
public record LlmReportResponse(
    String summary,
    List<String> strengths,
    List<String> improvements,
    List<String> recommendations,

    @JsonProperty("overall_score")
    Integer overallScore,

    @JsonProperty("question_feedbacks")
    List<QuestionFeedback> questionFeedbacks
) {
    public record QuestionFeedback(
        @JsonProperty("question_id")
        Long questionId,
        String feedback,
        Integer score
    ) {}
}
