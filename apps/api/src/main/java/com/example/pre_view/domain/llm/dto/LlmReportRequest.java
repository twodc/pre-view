package com.example.pre_view.domain.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM 리포트 요청 DTO
 */
public record LlmReportRequest(
    @JsonProperty("system_prompt")
    String systemPrompt,

    @JsonProperty("user_prompt")
    String userPrompt
) {
}
