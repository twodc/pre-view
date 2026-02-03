package com.example.pre_view.domain.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM 피드백 응답 DTO
 */
public record LlmFeedbackResponse(
    String feedback,
    Integer score,

    @JsonProperty("is_passed")
    Boolean isPassed,

    @JsonProperty("improvement_suggestion")
    String improvementSuggestion
) {
}
