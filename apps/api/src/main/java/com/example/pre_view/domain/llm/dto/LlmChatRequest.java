package com.example.pre_view.domain.llm.dto;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * LLM 채팅 요청 DTO
 *
 * Python LLM 서비스의 /api/chat 엔드포인트에 전송됩니다.
 */
public record LlmChatRequest(
        @NonNull List<Message> messages,
        @Nullable Double temperature,
        @Nullable Integer maxTokens
) {
    /**
     * 채팅 메시지
     */
    public record Message(
            @NonNull String role,
            @NonNull String content
    ) {}
}
