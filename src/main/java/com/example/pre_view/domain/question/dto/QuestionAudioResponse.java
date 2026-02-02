package com.example.pre_view.domain.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 질문 음성 응답 DTO
 */
@Schema(description = "질문 음성 응답")
public record QuestionAudioResponse(

        @Schema(description = "질문 ID", example = "1")
        Long questionId,

        @Schema(description = "질문 내용", example = "자기소개를 해주세요.")
        String content,

        @Schema(description = "Base64 인코딩된 오디오 데이터")
        String audioData,

        @Schema(description = "오디오 포맷 (wav 또는 mp3)", example = "wav")
        String audioFormat,

        @Schema(description = "오디오 길이 (초)", example = "3.5")
        Double duration,

        @Schema(description = "사용된 음성", example = "female_calm")
        String voice
) {
}
