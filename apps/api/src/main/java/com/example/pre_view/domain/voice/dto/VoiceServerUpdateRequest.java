package com.example.pre_view.domain.voice.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 음성 서버 설정 업데이트 요청
 */
public record VoiceServerUpdateRequest(
        Boolean enabled,

        @Pattern(regexp = "^(https?://.*)?$", message = "유효한 URL 형식이 아닙니다.")
        String gradioUrl
) {
}
