package com.example.pre_view.domain.interview.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.answer.dto.AnswerCreateRequest;
import com.example.pre_view.domain.answer.dto.AnswerResponse;
import com.example.pre_view.domain.answer.service.AnswerFacade;
import com.example.pre_view.domain.interview.dto.InterviewCreateRequest;
import com.example.pre_view.domain.interview.dto.InterviewResponse;
import com.example.pre_view.domain.interview.dto.InterviewResultResponse;
import com.example.pre_view.domain.interview.service.InterviewService;
import com.example.pre_view.domain.question.dto.QuestionListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 면접 관련 API 컨트롤러
 *
 * 면접 생성, 시작, 질문/답변 관리, 결과 조회 등 면접 전체 흐름을 담당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Tag(name = "Interview", description = "면접 API")
public class InterviewController {

    private final InterviewService interviewService;
    private final AnswerFacade answerFacade;

    @PostMapping
    @Operation(summary = "면접 생성", description = "새로운 면접을 생성합니다. (질문은 /start 호출 시 생성)")
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
        @Valid @RequestBody InterviewCreateRequest request
    ) {
        log.info("면접 생성 API 호출 - type: {}, position: {}, level: {}", 
                request.type(), request.position(), request.level());
        InterviewResponse response = interviewService.createInterview(request);
        log.info("면접 생성 완료 - interviewId: {}", response.id());
        return ResponseEntity.ok(ApiResponse.ok("면접이 생성되었습니다.", response));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "면접 시작", description = "면접을 시작하고 AI가 질문을 생성합니다.")
    public ResponseEntity<ApiResponse<InterviewResponse>> startInterview(
        @PathVariable("id") Long id
    ) {
        log.info("면접 시작 API 호출 - interviewId: {}", id);
        InterviewResponse response = interviewService.startInterview(id);
        log.info("면접 시작 완료 - interviewId: {}, totalQuestions: {}", id, response.totalQuestions());
        return ResponseEntity.ok(ApiResponse.ok("면접이 시작되었습니다. 질문이 생성되었습니다.", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "면접 조회", description = "면접 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(
        @PathVariable("id") Long id
    ) {
        log.info("면접 조회 API 호출 - interviewId: {}", id);
        InterviewResponse response = interviewService.getInterview(id);
        log.info("면접 조회 완료 - interviewId: {}, status: {}", id, response.status());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "면접 목록 조회", description = "전체 면접 목록을 페이징하여 조회합니다.")
    public ResponseEntity<ApiResponse<Page<InterviewResponse>>> getInterviews(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy
    ) {
        log.info("면접 목록 조회 API 호출 - page: {}, size: {}, sortBy: {}", page, size, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<InterviewResponse> responsePage = interviewService.getInterviews(pageable);
        log.info("면접 목록 조회 완료 - 총 {}개 (전체: {}개)", 
                responsePage.getNumberOfElements(), responsePage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(responsePage));
    }

    @GetMapping("/{id}/questions")
    @Operation(summary = "면접 질문 목록 조회", description = "면접의 질문 목록을 단계별로 조회합니다.")
    public ResponseEntity<ApiResponse<QuestionListResponse>> getQuestions(
        @PathVariable("id") Long id
    ) {
        log.info("면접 질문 목록 조회 API 호출 - interviewId: {}", id);
        QuestionListResponse response = interviewService.getQuestions(id);
        log.info("면접 질문 목록 조회 완료 - interviewId: {}, mainQuestions: {}, followUps: {}", 
                id, response.mainQuestionCount(), response.followUpCount());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/questions/{questionId}/answers")
    @Operation(summary = "답변 제출", description = "질문에 대한 답변을 제출하고 AI 피드백을 받습니다.")
    public ResponseEntity<ApiResponse<AnswerResponse>> createAnswer(
        @PathVariable("id") Long id,
        @PathVariable("questionId") Long questionId,
        @Valid @RequestBody AnswerCreateRequest request
    ) {
        log.info("답변 제출 API 호출 - interviewId: {}, questionId: {}", id, questionId);
        AnswerResponse response = answerFacade.createAnswer(id, questionId, request);
        log.info("답변 제출 완료 - interviewId: {}, questionId: {}, score: {}",
                id, questionId, response.score());
        return ResponseEntity.ok(ApiResponse.ok("답변이 제출되었습니다.", response));
    }

    @GetMapping("/{id}/result")
    @Operation(summary = "면접 결과 조회", description = "면접 결과와 AI 종합 리포트를 조회합니다.")
    public ResponseEntity<ApiResponse<InterviewResultResponse>> getInterviewResult(
        @PathVariable("id") Long id
    ) {
        log.info("면접 결과 조회 API 호출 - interviewId: {}", id);
        InterviewResultResponse response = interviewService.getInterviewResult(id);
        log.info("면접 결과 조회 완료 - interviewId: {}", id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping(value = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이력서 PDF 업로드", description = "이력서 PDF 파일을 업로드하고 텍스트를 추출합니다.")
    public ResponseEntity<ApiResponse<InterviewResponse>> uploadResume(
        @PathVariable("id") Long id,
        @RequestParam("file") MultipartFile file
    ) {
        log.info("이력서 업로드 API 호출 - interviewId: {}, 파일명: {}", id, file.getOriginalFilename());
        InterviewResponse response = interviewService.uploadResume(id, file);
        log.info("이력서 업로드 완료 - interviewId: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("이력서가 업로드되었습니다.", response));
    }

    @PostMapping(value = "/{id}/portfolio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "포트폴리오 PDF 업로드", description = "포트폴리오 PDF 파일을 업로드하고 텍스트를 추출합니다.")
    public ResponseEntity<ApiResponse<InterviewResponse>> uploadPortfolio(
        @PathVariable("id") Long id,
        @RequestParam("file") MultipartFile file
    ) {
        log.info("포트폴리오 업로드 API 호출 - interviewId: {}, 파일명: {}", id, file.getOriginalFilename());
        InterviewResponse response = interviewService.uploadPortfolio(id, file);
        log.info("포트폴리오 업로드 완료 - interviewId: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("포트폴리오가 업로드되었습니다.", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "면접 삭제", description = "면접을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteInterview(
        @PathVariable("id") Long id
    ) {
        log.info("면접 삭제 API 호출 - interviewId: {}", id);
        interviewService.deleteInterview(id);
        log.info("면접 삭제 완료 - interviewId: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("면접이 삭제되었습니다."));
    }
}
