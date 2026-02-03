package com.example.pre_view.domain.tts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * TTS 음성 합성 요청 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "음성 합성 요청")
public record SynthesizeRequest(

        @NotBlank(message = "텍스트는 필수입니다.")
        @Schema(description = "합성할 텍스트", example = "안녕하세요. 면접에 오신 것을 환영합니다.")
        String text,

        @Schema(description = "음성 스타일 (female_calm, male_professional 등)", example = "female_calm", defaultValue = "female_calm")
        String voice,

        @DecimalMin(value = "0.5", message = "속도는 0.5 이상이어야 합니다.")
        @DecimalMax(value = "2.0", message = "속도는 2.0 이하여야 합니다.")
        @Schema(description = "음성 속도 (0.5~2.0)", example = "1.0", defaultValue = "1.0")
        Double speed,

        @Pattern(regexp = "^(wav|mp3)$", message = "포맷은 wav 또는 mp3만 가능합니다.")
        @Schema(description = "출력 포맷 (wav 또는 mp3)", example = "wav", defaultValue = "wav", allowableValues = {"wav", "mp3"})
        String format
) {
    /**
     * 기본값이 적용된 생성자
     */
    public SynthesizeRequest {
        if (voice == null || voice.isBlank()) {
            voice = "female_calm";
        }
        if (speed == null) {
            speed = 1.0;
        }
        if (format == null || format.isBlank()) {
            format = "wav";
        }
    }

    /**
     * 간편 생성자 (텍스트만 필수)
     */
    public SynthesizeRequest(String text) {
        this(text, null, null, null);
    }
}
