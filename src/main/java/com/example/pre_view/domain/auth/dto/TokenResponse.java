package com.example.pre_view.domain.auth.dto;

import lombok.Builder;

/**
 * 토큰 응답 DTO
 */
@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static TokenResponse of(String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
