package com.example.pre_view.domain.llm.dto;

/**
 * LLM 면접 에이전트 응답 DTO
 */
public record LlmInterviewResponse(
    String thought,
    String action,      // FOLLOW_UP, NEW_TOPIC, NEXT_PHASE
    String message,     // 다음 질문 내용 (NEXT_PHASE가 아닐 때)
    String evaluation   // 현재 답변에 대한 간단한 평가
) {
}
