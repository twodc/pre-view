package com.example.pre_view.domain.interview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
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
import com.example.pre_view.domain.file.service.PdfExtractionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final AiInterviewService aiInterviewService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionTemplateService questionTemplateService;
    private final PdfExtractionService pdfExtractionService;

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

        // 템플릿 질문만 미리 생성 (인사, 자기소개, 마무리)
        List<Question> templateQuestions = new ArrayList<>();
        AtomicInteger sequence = new AtomicInteger(1);

        for (InterviewPhase phase : interview.getType().getPhases()) {
            if (phase.isTemplate()) {
                // 템플릿 질문 생성
                log.debug("템플릿 질문 생성 - interviewId: {}, phase: {}", interviewId, phase);
                List<String> templateQuestionTexts = questionTemplateService.getTemplateQuestions(phase);

                List<Question> phaseQuestions = templateQuestionTexts.stream()
                        .map(text -> Question.builder()
                                .content(text)
                                .interview(interview)
                                .phase(phase)
                                .sequence(sequence.getAndIncrement())
                                .isFollowUp(false)
                                .build())
                        .toList();

                templateQuestions.addAll(phaseQuestions);
                log.debug("템플릿 질문 생성 완료 - interviewId: {}, phase: {}, 질문 수: {}",
                        interviewId, phase, phaseQuestions.size());
            }
        }

        questionRepository.saveAll(templateQuestions);
        interview.start();

        log.info("면접 시작 처리 완료 - interviewId: {}, 템플릿 질문 수: {} (AI 질문은 실시간 생성)",
                interviewId, templateQuestions.size());
        return InterviewResponse.from(interview);
    }

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

    @Transactional
    public QuestionListResponse getQuestions(Long interviewId) {
        log.debug("질문 목록 조회 시작 - interviewId: {}", interviewId);

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        // 현재 저장된 질문들 조회
        List<Question> existingQuestions = questionRepository.findByInterviewIdOrderBySequence(interview.getId());

        // 현재 단계가 AI 질문 단계이고, 해당 단계에 질문이 없으면 생성
        InterviewPhase currentPhase = interview.getCurrentPhase();
        if (currentPhase != null && !currentPhase.isTemplate()) {
            boolean hasPhaseQuestion = existingQuestions.stream()
                    .anyMatch(q -> q.getPhase() == currentPhase && !q.isFollowUp());

            if (!hasPhaseQuestion) {
                log.info("현재 단계 AI 질문 생성 시작 - interviewId: {}, phase: {}", interviewId, currentPhase);
                Question aiQuestion = generateAiQuestion(interview, currentPhase, existingQuestions);
                if (aiQuestion != null) {
                    existingQuestions.add(aiQuestion);
                    log.info("AI 질문 생성 완료 - interviewId: {}, phase: {}, questionId: {}",
                            interviewId, currentPhase, aiQuestion.getId());
                }
            }
        }

        log.debug("질문 목록 조회 완료 - interviewId: {}, 총 질문 수: {}", interviewId, existingQuestions.size());
        return QuestionListResponse.of(interviewId, existingQuestions);
    }

    /**
     * AI 기반 질문 생성 (인성/태도, 기술 단계)
     */
    private Question generateAiQuestion(Interview interview, InterviewPhase phase, List<Question> existingQuestions) {
        try {
            String context = buildContext(interview);

            // 이전 답변들을 가져와서 컨텍스트에 추가 (더 개인화된 질문 생성)
            List<Answer> previousAnswers = answerRepository.findByInterviewId(interview.getId());
            List<String> previousAnswerTexts = previousAnswers.stream()
                    .map(Answer::getContent)
                    .toList();

            // AI 질문 생성
            String questionText = aiInterviewService.generateQuestionByContext(
                    phase,
                    context,
                    interview.getResumeText(),
                    interview.getPortfolioText(),
                    previousAnswerTexts);

            // 다음 시퀀스 번호 계산
            int nextSequence = existingQuestions.isEmpty()
                    ? 1
                    : existingQuestions.stream()
                            .mapToInt(q -> q.getSequence() != null ? q.getSequence() : 0)
                            .max()
                            .orElse(0) + 1;

            Question question = Question.builder()
                    .content(questionText)
                    .interview(interview)
                    .phase(phase)
                    .sequence(nextSequence)
                    .isFollowUp(false)
                    .build();

            return questionRepository.save(question);
        } catch (Exception e) {
            log.error("AI 질문 생성 실패 - interviewId: {}, phase: {}", interview.getId(), phase, e);
            return null;
        }
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

    @Transactional
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

        String reportContext = buildContext(interview);
        log.debug("AI 리포트 생성 시작 - interviewId: {}", id);
        AiReportResponse report = aiInterviewService.generateReport(reportContext, answers);
        log.info("AI 리포트 생성 완료 - interviewId: {}", id);

        InterviewResultResponse response = InterviewResultResponse.of(interview, questions, answers, report);

        // 결과 조회 시 면접 완료 처리
        if (interview.getStatus() != InterviewStatus.DONE) {
            interview.complete();
        }

        log.info("면접 결과 조회 완료 - interviewId: {}", id);
        return response;
    }

    /**
     * 이력서 PDF 파일 업로드 및 텍스트 추출
     */
    @Transactional
    public InterviewResponse uploadResume(Long interviewId, MultipartFile file) {
        log.info("이력서 업로드 시작 - interviewId: {}, 파일명: {}", interviewId, file.getOriginalFilename());

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        // PDF에서 텍스트 추출
        String resumeText = pdfExtractionService.extractText(file);

        // 면접 엔티티에 텍스트 저장
        interview.updateResumeText(resumeText);

        log.info("이력서 업로드 완료 - interviewId: {}, 추출된 텍스트 길이: {} 문자",
                interviewId, resumeText.length());

        return InterviewResponse.from(interview);
    }

    /**
     * 포트폴리오 PDF 파일 업로드 및 텍스트 추출
     */
    @Transactional
    public InterviewResponse uploadPortfolio(Long interviewId, MultipartFile file) {
        log.info("포트폴리오 업로드 시작 - interviewId: {}, 파일명: {}", interviewId, file.getOriginalFilename());

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        // PDF에서 텍스트 추출
        String portfolioText = pdfExtractionService.extractText(file);

        // 면접 엔티티에 텍스트 저장
        interview.updatePortfolioText(portfolioText);

        log.info("포트폴리오 업로드 완료 - interviewId: {}, 추출된 텍스트 길이: {} 문자",
                interviewId, portfolioText.length());

        return InterviewResponse.from(interview);
    }
}
