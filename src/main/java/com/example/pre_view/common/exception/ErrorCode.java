package com.example.pre_view.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    // Interview
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "면접을 찾을 수 없습니다."),
    INVALID_INTERVIEW_STATUS(HttpStatus.BAD_REQUEST, "I002", "현재 면접 상태에서는 해당 작업을 수행할 수 없습니다."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "질문을 찾을 수 없습니다."),
    INVALID_QUESTION_PHASE(HttpStatus.BAD_REQUEST, "Q002", "템플릿 질문이 없는 단계입니다. 템플릿 질문은 GREETING, SELF_INTRO, CLOSING만 지원합니다."),

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
