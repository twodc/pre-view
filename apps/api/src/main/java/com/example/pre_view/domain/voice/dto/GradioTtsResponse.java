package com.example.pre_view.domain.voice.dto;

/**
 * Gradio TTS 응답 DTO
 */
public record GradioTtsResponse(
    String audioData,  // Base64 인코딩된 오디오 또는 URL
    String format,
    int sampleRate
) {}
