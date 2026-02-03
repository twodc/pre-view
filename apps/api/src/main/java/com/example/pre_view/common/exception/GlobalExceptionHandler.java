package com.example.pre_view.common.exception;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 *
 * 애플리케이션에서 발생하는 모든 예외를 중앙에서 처리하고,
 * 일관된 형식의 에러 응답을 반환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException 발생: {}", e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        log.error("MethodArgumentNotValidException 발생: {}", e.getMessage(), e);
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException e) {
        log.warn("낙관적 락 실패 - 동시성 충돌 발생: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.CONCURRENT_MODIFICATION.getStatus())
                .body(ErrorResponse.of(ErrorCode.CONCURRENT_MODIFICATION));
    }

    @ExceptionHandler(InterviewStateException.class)
    public ResponseEntity<ErrorResponse> handleInterviewStateException(InterviewStateException e) {
        log.warn("잘못된 면접 상태 요청: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INTERVIEW_STATUS.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INTERVIEW_STATUS, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception 발생: {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
