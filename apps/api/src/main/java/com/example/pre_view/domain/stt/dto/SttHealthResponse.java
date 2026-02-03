package com.example.pre_view.domain.stt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * STT 서비스 헬스체크 응답 DTO
 */
@Schema(description = "STT 서비스 상태")
public record SttHealthResponse(

        @Schema(description = "서비스 상태", example = "healthy")
        @JsonProperty("status")
        String status,

        @Schema(description = "모델 로딩 여부", example = "true")
        @JsonProperty("model_loaded")
        Boolean modelLoaded,

        @Schema(description = "사용 중인 디바이스 (GPU/CPU)", example = "cuda")
        @JsonProperty("device")
        String device
) {
}
