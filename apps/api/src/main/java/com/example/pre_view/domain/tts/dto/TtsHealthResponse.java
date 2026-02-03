package com.example.pre_view.domain.tts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * TTS 서비스 헬스체크 응답 DTO
 */
@Schema(description = "TTS 서비스 헬스체크 응답")
public record TtsHealthResponse(

        @Schema(description = "서비스 상태", example = "healthy")
        String status,

        @JsonProperty("model_loaded")
        @Schema(description = "모델 로딩 여부", example = "true")
        Boolean modelLoaded,

        @Schema(description = "사용 중인 디바이스 (GPU/CPU)", example = "cuda")
        String device
) {
}
