package com.example.pre_view.domain.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM 피드백 요청 DTO
 */
public record LlmFeedbackRequest(
    @JsonProperty("system_prompt")
    String systemPrompt,

    @JsonProperty("user_prompt")
    String userPrompt
) {
}
