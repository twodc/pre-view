package com.example.pre_view.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 정의
 *
 * 도메인별로 에러 코드를 분류하여 관리합니다.
 * 각 코드는 HTTP 상태, 고유 코드, 메시지로 구성됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    // 인증/인가 (Auth)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH004", "유효하지 않은 Refresh Token입니다."),
    INVALID_TOKEN_TYPE(HttpStatus.BAD_REQUEST, "AUTH005", "잘못된 토큰 타입입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH006", "접근 권한이 없습니다."),

    // 회원 (Member)
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_LOGIN_METHOD(HttpStatus.BAD_REQUEST, "M004", "소셜 로그인으로 가입된 계정입니다."),

    // Interview
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "면접을 찾을 수 없습니다."),
    INVALID_INTERVIEW_STATUS(HttpStatus.BAD_REQUEST, "I002", "현재 면접 상태에서는 해당 작업을 수행할 수 없습니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "I003", "동시에 수정되어 처리할 수 없습니다. 다시 시도해주세요."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "질문을 찾을 수 없습니다."),
    INVALID_QUESTION_PHASE(HttpStatus.BAD_REQUEST, "Q002", "템플릿 질문이 없는 단계입니다. 템플릿 질문은 OPENING, CLOSING만 지원합니다."),

    // Answer
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "답변을 찾을 수 없습니다."),

    // AI
    AI_RESPONSE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 응답 처리 중 오류가 발생했습니다."),
    
    // File
    FILE_UPLOAD_ERROR(HttpStatus.BAD_REQUEST, "F001", "파일 업로드 중 오류가 발생했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "F002", "지원하지 않는 파일 형식입니다. PDF 파일만 업로드 가능합니다."),
    FILE_READ_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 읽기 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
