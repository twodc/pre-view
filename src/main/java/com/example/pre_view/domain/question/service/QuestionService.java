package com.example.pre_view.domain.question.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.interview.service.AiInterviewService;
import com.example.pre_view.domain.question.dto.QuestionListResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 질문 관련 비즈니스 로직을 처리하는 서비스
 * - 템플릿 질문 생성
 * - AI 질문 생성
 * - 질문 목록 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AnswerRepository answerRepository;
    private final QuestionTemplateService questionTemplateService;
    private final AiInterviewService aiInterviewService;

    /**
     * 면접 시작 시 템플릿 질문들을 생성합니다.
     * 인사, 자기소개, 마무리 단계의 고정 질문들을 미리 생성합니다.
     * 
     * @param interview 면접 엔티티
     * @return 생성된 템플릿 질문 목록
     */
    @Transactional
    public List<Question> createTemplateQuestions(Interview interview) {
        log.debug("템플릿 질문 생성 시작 - interviewId: {}", interview.getId());

        List<Question> templateQuestions = new ArrayList<>();
        AtomicInteger sequence = new AtomicInteger(1);

        // 면접 타입에 따른 단계별로 템플릿 질문 생성
        for (InterviewPhase phase : interview.getType().getPhases()) {
            if (phase.isTemplate()) {
                log.debug("템플릿 질문 생성 - interviewId: {}, phase: {}", interview.getId(), phase);
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
                        interview.getId(), phase, phaseQuestions.size());
            }
        }

        List<Question> savedQuestions = questionRepository.saveAll(templateQuestions);
        log.info("템플릿 질문 생성 완료 - interviewId: {}, 총 질문 수: {}",
                interview.getId(), savedQuestions.size());

        return savedQuestions;
    }

    /**
     * 면접의 질문 목록을 조회합니다.
     * 현재 단계가 AI 질문 단계이고 질문이 없으면 자동으로 생성합니다.
     * 
     * @param interviewId 면접 ID
     * @return 질문 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    public QuestionListResponse getQuestions(Long interviewId) {
        log.debug("질문 목록 조회 시작 - interviewId: {}", interviewId);

        Interview interview = interviewRepository.findByIdAndNotDeleted(interviewId)
                .orElseThrow(() -> {
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        List<Question> existingQuestions = questionRepository.findByInterviewIdOrderBySequence(interview.getId());

        // 현재 단계가 AI 질문 단계이고, 해당 단계에 질문이 없으면 생성
        InterviewPhase currentPhase = interview.getCurrentPhase();
        if (currentPhase != null && !currentPhase.isTemplate()) {
            boolean hasPhaseQuestion = existingQuestions.stream()
                    .anyMatch(q -> q.getPhase() == currentPhase && !q.isFollowUp());

            if (!hasPhaseQuestion) {
                log.info("현재 단계 AI 질문 생성 시작 - interviewId: {}, phase: {}", interviewId, currentPhase);
                Question aiQuestion = generateAiQuestionInNewTransaction(interview, currentPhase, existingQuestions);
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
     * 이력서, 포트폴리오, 이전 답변을 기반으로 개인화된 질문을 생성합니다.
     * 
     * @param interview 면접 엔티티
     * @param phase 면접 단계
     * @param existingQuestions 기존 질문 목록 (시퀀스 번호 계산용)
     * @return 생성된 질문 엔티티 (실패 시 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Question generateAiQuestionInNewTransaction(Interview interview, InterviewPhase phase, List<Question> existingQuestions) {
        String questionText = generateAiQuestionText(interview, phase);
        
        if (questionText == null) {
            return null;
        }

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
    }

    /**
     * AI 질문 텍스트 생성
     */
    private String generateAiQuestionText(Interview interview, InterviewPhase phase) {
        try {
            String context = buildContext(interview);

            // 이전 답변들을 가져와서 컨텍스트에 추가 (더 개인화된 질문 생성)
            List<Answer> previousAnswers = answerRepository.findByInterviewIdWithQuestion(interview.getId());
            List<String> previousAnswerTexts = previousAnswers.stream()
                    .map(Answer::getContent)
                    .toList();

            return aiInterviewService.generateQuestionByContext(
                    phase,
                    context,
                    interview.getResumeText(),
                    interview.getPortfolioText(),
                    previousAnswerTexts);
        } catch (Exception e) {
            log.error("AI 질문 생성 실패 - interviewId: {}, phase: {}", interview.getId(), phase, e);
            return null;
        }
    }

    /**
     * 면접 컨텍스트 문자열 생성
     * 포지션, 레벨, 기술 스택 정보를 조합합니다.
     * 
     * @param interview 면접 엔티티
     * @return 컨텍스트 문자열
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
}

