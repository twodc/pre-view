package com.example.pre_view.domain.voice.dto;

/**
 * 음성 서버 상태 응답
 */
public record VoiceServerStatus(
        boolean enabled,
        String gradioUrl,
        boolean available,
        boolean healthy,
        String message
) {
}
