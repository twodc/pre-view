package com.example.pre_view.common.exception;

/**
 * 면접 상태 관련 예외
 * IllegalStateException 대신 사용하여 명확한 예외 처리
 */
public class InterviewStateException extends RuntimeException {
    public InterviewStateException(String message) {
        super(message);
    }
}
