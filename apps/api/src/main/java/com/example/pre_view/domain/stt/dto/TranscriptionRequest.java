package com.example.pre_view.domain.stt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * STT 전사 요청 DTO
 */
@Schema(description = "음성 인식 요청")
public record TranscriptionRequest(

        @Schema(description = "언어 코드 (ko, en 등)", example = "ko", defaultValue = "ko")
        @Pattern(regexp = "^(ko|en|ja|zh)$", message = "지원하지 않는 언어입니다. (ko, en, ja, zh만 가능)")
        @JsonProperty("language")
        String language
) {
    /**
     * 기본 생성자 - 언어 기본값 "ko" 설정
     */
    public TranscriptionRequest {
        if (language == null || language.isBlank()) {
            language = "ko";
        }
    }
}
