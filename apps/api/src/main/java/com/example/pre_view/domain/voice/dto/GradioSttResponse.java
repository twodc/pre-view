package com.example.pre_view.domain.voice.dto;

/**
 * Gradio STT 응답 DTO
 */
public record GradioSttResponse(
    String text,
    String language,
    Double confidence
) {}
