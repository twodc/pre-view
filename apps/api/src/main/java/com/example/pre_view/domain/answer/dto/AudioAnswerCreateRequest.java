package com.example.pre_view.domain.answer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 음성 답변 생성 요청 DTO
 */
@Schema(description = "음성 답변 생성 요청")
public record AudioAnswerCreateRequest(

        @Schema(description = "언어 코드 (ko, en, ja, zh)", example = "ko", defaultValue = "ko")
        @NotBlank(message = "언어 코드는 필수입니다.")
        @Pattern(regexp = "^(ko|en|ja|zh)$", message = "지원하지 않는 언어입니다. (ko, en, ja, zh만 가능)")
        String language
) {
    /**
     * 기본 생성자 - 언어 기본값 "ko" 설정
     */
    public AudioAnswerCreateRequest {
        if (language == null || language.isBlank()) {
            language = "ko";
        }
    }
}
