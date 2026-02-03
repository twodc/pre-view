package com.example.pre_view.domain.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM 면접 에이전트 요청 DTO
 */
public record LlmInterviewRequest(
    @JsonProperty("system_prompt")
    String systemPrompt,

    @JsonProperty("user_prompt")
    String userPrompt
) {
}
