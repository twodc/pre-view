package com.example.pre_view.domain.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * TTS 음성 정보 DTO
 */
@Schema(description = "음성 정보")
public record VoiceInfo(

        @Schema(description = "음성 ID", example = "female_calm")
        String id,

        @Schema(description = "음성 이름", example = "차분한 여성")
        String name,

        @Schema(description = "성별", example = "female")
        String gender,

        @Schema(description = "설명", example = "차분하고 전문적인 여성 음성")
        String description
) {
}
