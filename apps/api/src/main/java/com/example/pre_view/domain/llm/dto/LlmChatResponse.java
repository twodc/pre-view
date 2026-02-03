package com.example.pre_view.domain.llm.dto;

import org.jspecify.annotations.NonNull;

/**
 * LLM 채팅 응답 DTO
 *
 * Python LLM 서비스의 /api/chat 엔드포인트로부터 수신됩니다.
 */
public record LlmChatResponse(
        @NonNull String content,
        @NonNull Usage usage
) {
    /**
     * 토큰 사용량
     */
    public record Usage(
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {}
}
