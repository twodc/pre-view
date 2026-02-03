package com.example.pre_view.domain.tts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * TTS 음성 합성 응답 DTO
 */
@Schema(description = "음성 합성 응답")
public record SynthesizeResponse(

        @JsonProperty("audio_data")
        @Schema(description = "Base64 인코딩된 오디오 데이터", example = "UklGRiQAAABXQVZFZm10...")
        String audioData,

        @Schema(description = "오디오 포맷", example = "wav")
        String format,

        @Schema(description = "오디오 길이 (초)", example = "3.5")
        Double duration,

        @JsonProperty("sample_rate")
        @Schema(description = "샘플레이트 (Hz)", example = "22050")
        Integer sampleRate
) {
}
