package com.example.pre_view.common.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {
    // 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "성공", data, LocalDateTime.now());
    }

    // 성공 응답 (커스텀 메시지 + 데이터)
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    // 성공 응답 (메시지만, 데이터 없음)
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }
}
