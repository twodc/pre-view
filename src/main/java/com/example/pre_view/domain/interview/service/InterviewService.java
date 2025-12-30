package com.example.pre_view.domain.interview.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
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

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
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
    public List<InterviewResponse> getInterviews() {
        log.debug("면접 목록 조회 시작");
        List<InterviewResponse> responses = interviewRepository.findAllActive().stream()
                .map(InterviewResponse::from)
                .collect(Collectors.toList());
        log.info("면접 목록 조회 완료 - 총 {}개", responses.size());
        return responses;
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

        Interview interview = interviewRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", id);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        List<Question> questions = questionRepository.findByInterviewId(interview.getId());
        List<Answer> answers = answerRepository.findByInterviewId(interview.getId());
        log.debug("면접 데이터 조회 완료 - interviewId: {}, 질문: {}개, 답변: {}개",
                id, questions.size(), answers.size());

        // AI 리포트 생성은 트랜잭션 밖에서 수행 (외부 API 호출)
        String reportContext = buildContext(interview);
        log.debug("AI 리포트 생성 시작 - interviewId: {}", id);
        AiReportResponse report = aiInterviewService.generateReport(reportContext, answers);
        log.info("AI 리포트 생성 완료 - interviewId: {}", id);

        InterviewResultResponse response = InterviewResultResponse.of(interview, questions, answers, report);

        // 결과 조회 시 면접 완료 처리 (별도 트랜잭션으로 수행)
        completeInterviewIfNeeded(id, interview);

        log.info("면접 결과 조회 완료 - interviewId: {}", id);
        return response;
    }

    /**
     * 면접 컨텍스트 문자열 생성 (AI 리포트 생성용)
     */
    private String buildContext(Interview interview) {
        StringBuilder context = new StringBuilder();
        context.append(interview.getPosition().getDescription())
                .append(" ")
                .append(interview.getLevel().getDescription());

        if (interview.getTechStacks() != null && !interview.getTechStacks().isEmpty()) {
            context.append(" (")
                    .append(String.join(", ", interview.getTechStacks()))
                    .append(")");
        }

        return context.toString();
    }

    /**
     * 면접 완료 처리 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void completeInterviewIfNeeded(Long id, Interview interview) {
        Interview latestInterview = interviewRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        
        if (latestInterview.getStatus() != InterviewStatus.DONE) {
            latestInterview.complete();
            log.info("면접 완료 처리 - interviewId: {}", id);
        }
    }

    /**
     * 이력서 업로드는 FileUploadService에 위임
     * FileUploadService에 이미 @Transactional이 있으므로 중복 제거
     */
    public InterviewResponse uploadResume(Long interviewId, MultipartFile file) {
        return fileUploadService.uploadResume(interviewId, file);
    }

    /**
     * 포트폴리오 업로드는 FileUploadService에 위임
     * FileUploadService에 이미 @Transactional이 있으므로 중복 제거
     */
    public InterviewResponse uploadPortfolio(Long interviewId, MultipartFile file) {
        return fileUploadService.uploadPortfolio(interviewId, file);
    }
}
