package com.example.pre_view.domain.stt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * STT 전사 응답 DTO
 */
@Schema(description = "음성 인식 결과")
public record TranscriptionResponse(

        @Schema(description = "전사된 텍스트", example = "안녕하세요, 저는 백엔드 개발자입니다.")
        @JsonProperty("text")
        String text,

        @Schema(description = "감지된 언어", example = "ko")
        @JsonProperty("language")
        String language,

        @Schema(description = "오디오 길이 (초)", example = "5.2")
        @JsonProperty("duration")
        Double duration,

        @Schema(description = "신뢰도 (0~1)", example = "0.95")
        @JsonProperty("confidence")
        Double confidence
) {
}
