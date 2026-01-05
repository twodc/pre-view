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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    @Transactional
    public InterviewResponse createInterview(InterviewCreateRequest requestDto) {
        log.debug("면접 엔티티 생성 시작 - type: {}, position: {}, level: {}",
                requestDto.type(), requestDto.position(), requestDto.level());
        Interview interview = interviewRepository.save(requestDto.toEntity());
        log.info("면접 생성 완료 - interviewId: {}", interview.getId());
        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse startInterview(Long interviewId) {
        log.info("면접 시작 처리 시작 - interviewId: {}", interviewId);

        Interview interview = interviewRepository.findByIdAndDeletedFalse(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

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
    public QuestionListResponse getQuestions(Long interviewId) {
        return questionService.getQuestions(interviewId);
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(Long interviewId) {
        log.debug("면접 조회 시작 - interviewId: {}", interviewId);

        Interview interview = interviewRepository.findByIdAndDeletedFalse(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        log.debug("면접 조회 성공 - interviewId: {}, status: {}, type: {}",
                interview.getId(), interview.getStatus(), interview.getType());

        InterviewResponse response = InterviewResponse.from(interview);
        log.info("면접 조회 완료 - interviewId: {}", interviewId);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviews(Pageable pageable) {
        log.debug("면접 목록 조회 시작 - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<InterviewResponse> responsePage = interviewRepository.findAllByDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(InterviewResponse::from);
        log.info("면접 목록 조회 완료 - 총 {}개 (전체: {}개)", 
                responsePage.getNumberOfElements(), responsePage.getTotalElements());
        return responsePage;
    }

    @Transactional
    public void deleteInterview(Long interviewId) {
        log.info("면접 삭제 처리 시작 - interviewId: {}", interviewId);

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        interview.delete();
        log.info("면접 삭제 처리 완료 - interviewId: {}", interviewId);
    }

    /**
     * 면접 결과 조회 및 AI 리포트 생성
     */
    @Transactional(readOnly = true)
    public InterviewResultResponse getInterviewResult(Long id) {
        log.info("면접 결과 조회 시작 - interviewId: {}", id);

        Interview interview = interviewRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", id);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        List<Question> questions = questionRepository
                .findByInterviewIdOrderBySequence(interview.getId());
        List<Answer> answers = answerRepository
                .findByInterviewIdWithQuestion(interview.getId());
        log.debug("면접 데이터 조회 완료 - interviewId: {}, 질문: {}개, 답변: {}개",
                id, questions.size(), answers.size());

        // AI 리포트 생성은 트랜잭션 밖에서 수행 (외부 API 호출)
        log.debug("AI 리포트 생성 시작 - interviewId: {}", id);
        AiReportResponse report = aiInterviewService.generateReport(interview.buildContext(), answers);
        log.info("AI 리포트 생성 완료 - interviewId: {}", id);

        InterviewResultResponse response = InterviewResultResponse.of(interview, questions, answers, report);

        // 결과 조회 시 면접 완료 처리 (별도 서비스의 새 트랜잭션으로 수행)
        // 주의: Spring AOP 프록시는 private 메서드에 적용되지 않으므로,
        // REQUIRES_NEW 전파가 필요한 로직은 별도 서비스로 분리하였습니다.
        interviewStatusService.completeInterviewIfNeeded(id);

        log.info("면접 결과 조회 완료 - interviewId: {}", id);
        return response;
    }

    public InterviewResponse uploadResume(Long interviewId, MultipartFile file) {
        return fileUploadService.uploadResume(interviewId, file);
    }

    public InterviewResponse uploadPortfolio(Long interviewId, MultipartFile file) {
        return fileUploadService.uploadPortfolio(interviewId, file);
    }
}
