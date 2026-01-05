package com.example.pre_view.domain.answer.dto;

public record AiFeedbackResponse(
    String feedback,
    Integer score,
    boolean needsFollowUp,
    String followUpQuestion
) {
}
