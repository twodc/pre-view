package com.example.pre_view.domain.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerCreateRequest(
    @NotBlank(message = "답변 내용은 필수 입력 값입니다.")
    String content
) {
}
