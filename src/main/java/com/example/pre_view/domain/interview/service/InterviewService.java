package com.example.pre_view.domain.interview.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.file.service.FileUploadService;
import com.example.pre_view.domain.interview.dto.AiReportResponse;
import com.example.pre_view.domain.interview.dto.InterviewCreateRequest;
import com.example.pre_view.domain.interview.dto.InterviewResponse;
import com.example.pre_view.domain.interview.dto.InterviewResultResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.question.dto.QuestionListResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;
import com.example.pre_view.domain.question.service.QuestionService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 면접 관련 비즈니스 로직
 *
 * 면접 생성, 시작, 조회, 삭제 및 AI 리포트 생성을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final AiInterviewService aiInterviewService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionService questionService;
    private final FileUploadService fileUploadService;
    private final InterviewStatusService interviewStatusService;
    private final JsonMapper jsonMapper;

    @Transactional
    public InterviewResponse createInterview(InterviewCreateRequest requestDto, Long memberId) {
        log.debug("면접 엔티티 생성 시작 - type: {}, position: {}, level: {}, memberId: {}",
                requestDto.type(), requestDto.position(), requestDto.level(), memberId);
        Interview interview = interviewRepository.save(requestDto.toEntity(memberId));
        log.info("면접 생성 완료 - interviewId: {}, memberId: {}", interview.getId(), memberId);
        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse startInterview(Long interviewId, Long memberId) {
        log.info("면접 시작 처리 시작 - interviewId: {}, memberId: {}", interviewId, memberId);

        Interview interview = getInterviewWithAuth(interviewId, memberId);

        if (interview.getStatus() != InterviewStatus.READY) {
            log.warn("잘못된 면접 상태 - interviewId: {}, 현재 상태: {}", interviewId, interview.getStatus());
            throw new BusinessException(ErrorCode.INVALID_INTERVIEW_STATUS);
        }

        List<Question> templateQuestions = questionService.createTemplateQuestions(interview);
        interview.start();

        // 첫 단계가 AI 생성 단계(TECHNICAL, PERSONALITY)면 첫 질문 생성
        InterviewPhase firstPhase = interview.getCurrentPhase();
        if (firstPhase != null && !firstPhase.isTemplate()) {
            log.info("AI 생성 단계로 시작 - interviewId: {}, phase: {}", interviewId, firstPhase);
            questionService.generateFirstQuestionByAgent(interview, null, firstPhase);
        }

        log.info("면접 시작 처리 완료 - interviewId: {}, 템플릿 질문 수: {} (AI 질문은 실시간 생성)",
                interviewId, templateQuestions.size());
        return InterviewResponse.from(interview);
    }

    @Transactional
    public QuestionListResponse getQuestions(Long interviewId, Long memberId) {
        // 권한 검증
        getInterviewWithAuth(interviewId, memberId);
        return questionService.getQuestions(interviewId);
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(Long interviewId, Long memberId) {
        log.debug("면접 조회 시작 - interviewId: {}, memberId: {}", interviewId, memberId);

        Interview interview = getInterviewWithAuth(interviewId, memberId);

        log.debug("면접 조회 성공 - interviewId: {}, status: {}, type: {}",
                interview.getId(), interview.getStatus(), interview.getType());

        InterviewResponse response = InterviewResponse.from(interview);
        log.info("면접 조회 완료 - interviewId: {}", interviewId);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviews(Long memberId, Pageable pageable) {
        log.debug("면접 목록 조회 시작 - memberId: {}, page: {}, size: {}",
                memberId, pageable.getPageNumber(), pageable.getPageSize());
        Page<InterviewResponse> responsePage = interviewRepository
                .findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(memberId, pageable)
                .map(InterviewResponse::from);
        log.info("면접 목록 조회 완료 - memberId: {}, 총 {}개 (전체: {}개)",
                memberId, responsePage.getNumberOfElements(), responsePage.getTotalElements());
        return responsePage;
    }

    @Transactional
    public void deleteInterview(Long interviewId, Long memberId) {
        log.info("면접 삭제 처리 시작 - interviewId: {}, memberId: {}", interviewId, memberId);

        Interview interview = getInterviewWithAuth(interviewId, memberId);

        interview.delete();
        log.info("면접 삭제 처리 완료 - interviewId: {}", interviewId);
    }

    /**
     * 면접 결과 조회 및 AI 리포트 생성 (캐싱 적용)
     *
     * 최초 조회 시 AI 리포트를 생성하고 DB에 캐싱합니다.
     * 이후 조회 시에는 캐싱된 리포트를 반환하여 AI API 호출을 줄입니다.
     */
    @Transactional(readOnly = true)
    public InterviewResultResponse getInterviewResult(Long id, Long memberId) {
        log.info("면접 결과 조회 시작 - interviewId: {}, memberId: {}", id, memberId);

        Interview interview = getInterviewWithAuth(id, memberId);

        List<Question> questions = questionRepository
                .findByInterviewIdOrderBySequence(interview.getId());
        List<Answer> answers = answerRepository
                .findByInterviewIdWithQuestion(interview.getId());
        log.debug("면접 데이터 조회 완료 - interviewId: {}, 질문: {}개, 답변: {}개",
                id, questions.size(), answers.size());

        // 저장된 리포트가 있으면 사용, 없으면 AI 생성 후 저장
        AiReportResponse report;
        if (interview.hasAiReport()) {
            log.info("저장된 AI 리포트 사용 - interviewId: {}", id);
            report = deserializeReport(interview.getAiReport());
        } else {
            log.debug("AI 리포트 생성 시작 - interviewId: {}", id);
            report = aiInterviewService.generateReport(interview.buildContext(), answers);
            log.info("AI 리포트 생성 완료 - interviewId: {}", id);

            // 별도 트랜잭션으로 리포트 저장
            interviewStatusService.saveAiReport(id, serializeReport(report));
        }

        InterviewResultResponse response = InterviewResultResponse.of(interview, questions, answers, report);

        // 결과 조회 시 면접 완료 처리 (별도 서비스의 새 트랜잭션으로 수행)
        interviewStatusService.completeInterviewIfNeeded(id);

        log.info("면접 결과 조회 완료 - interviewId: {}", id);
        return response;
    }

    /**
     * AiReportResponse를 JSON 문자열로 직렬화
     */
    private String serializeReport(AiReportResponse report) {
        try {
            return jsonMapper.writeValueAsString(report);
        } catch (JacksonException e) {
            log.error("리포트 직렬화 실패", e);
            return null;
        }
    }

    /**
     * JSON 문자열을 AiReportResponse로 역직렬화
     */
    private AiReportResponse deserializeReport(String json) {
        try {
            return jsonMapper.readValue(json, AiReportResponse.class);
        } catch (JacksonException e) {
            log.error("리포트 역직렬화 실패", e);
            return null;
        }
    }

    public InterviewResponse uploadResume(Long interviewId, Long memberId, MultipartFile file) {
        getInterviewWithAuth(interviewId, memberId);
        return fileUploadService.uploadResume(interviewId, file);
    }

    public InterviewResponse uploadPortfolio(Long interviewId, Long memberId, MultipartFile file) {
        getInterviewWithAuth(interviewId, memberId);
        return fileUploadService.uploadPortfolio(interviewId, file);
    }

    /**
     * 면접 조회 및 권한 검증 공통 메서드
     *
     * @param interviewId 면접 ID
     * @param memberId 현재 사용자 ID
     * @return Interview 엔티티
     * @throws BusinessException INTERVIEW_NOT_FOUND(면접 없음) 또는 ACCESS_DENIED(권한 없음)
     */
    private Interview getInterviewWithAuth(Long interviewId, Long memberId) {
        return interviewRepository.findByIdAndMemberIdAndDeletedFalse(interviewId, memberId)
                .orElseThrow(() -> {
                    // 면접이 존재하는지 먼저 확인하여 적절한 에러 반환
                    boolean exists = interviewRepository.findByIdAndDeletedFalse(interviewId).isPresent();
                    if (exists) {
                        log.warn("면접 접근 권한 없음 - interviewId: {}, memberId: {}", interviewId, memberId);
                        return new BusinessException(ErrorCode.ACCESS_DENIED);
                    }
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });
    }
}
