package com.example.pre_view.domain.answer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.dto.AnswerResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.dto.AiInterviewAgentResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewAction;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.question.dto.QuestionResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;
import com.example.pre_view.domain.question.service.QuestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 답변 저장 관련 DB 작업을 처리하는 서비스
 *
 * 역할:
 * - 답변 엔티티 저장
 * - Agent 결과에 따른 질문 생성/단계 전환
 * - 트랜잭션 관리
 *
 * 참고: 외부 API 호출은 AnswerFacade에서 수행하고, 이 서비스는 순수 DB 작업만 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;

    /**
     * Template 단계용 답변 저장 (OPENING, CLOSING)
     * Agent 호출 없이 피드백만 저장하고, 단계 전환을 체크합니다.
     *
     * @param question 질문 엔티티
     * @param content 답변 내용
     * @param aiFeedback AI 피드백 응답
     * @return 답변 응답 DTO
     */
    @Transactional
    public AnswerResponse saveAnswerForTemplate(Question question, String content, AiFeedbackResponse aiFeedback) {
        Answer savedAnswer = saveAnswer(question, content, aiFeedback);

        // Template 단계 전환 체크
        checkAndMoveToNextPhaseForTemplate(question.getInterview(), question.getPhase());

        return AnswerResponse.from(savedAnswer);
    }

    /**
     * AI 생성 단계용 답변 저장 및 Agent 결과 처리 (TECHNICAL, PERSONALITY)
     * Agent의 판단에 따라 새 질문을 생성하거나 다음 단계로 전환합니다.
     *
     * @param question 질문 엔티티
     * @param content 답변 내용
     * @param aiFeedback AI 피드백 응답
     * @param agentResponse Agent 응답 (action, message 포함)
     * @return 답변 응답 DTO
     */
    @Transactional
    public AnswerResponse saveAnswerWithAgentResult(
            Question question,
            String content,
            AiFeedbackResponse aiFeedback,
            AiInterviewAgentResponse agentResponse
    ) {
        Answer savedAnswer = saveAnswer(question, content, aiFeedback);
        Interview interview = question.getInterview();
        InterviewPhase currentPhase = question.getPhase();

        // Agent 결정에 따른 분기
        if (agentResponse.action() == InterviewAction.NEXT_PHASE) {
            return handleNextPhase(interview, currentPhase, savedAnswer);
        } else {
            return handleGenerateQuestion(interview, currentPhase, question, agentResponse, savedAnswer);
        }
    }

    /**
     * 답변 저장 공통 로직
     */
    private Answer saveAnswer(Question question, String content, AiFeedbackResponse aiFeedback) {
        Answer answer = Answer.builder()
                .question(question)
                .content(content)
                .feedback(aiFeedback.feedback())
                .score(aiFeedback.score())
                .build();

        Answer savedAnswer = answerRepository.save(answer);
        question.markAsAnswered();

        log.info("답변 저장 완료 - questionId: {}, answerId: {}, score: {}",
                question.getId(), savedAnswer.getId(), savedAnswer.getScore());

        return savedAnswer;
    }

    /**
     * NEXT_PHASE 액션 처리 - 다음 단계로 전환
     */
    private AnswerResponse handleNextPhase(Interview interview, InterviewPhase currentPhase, Answer savedAnswer) {
        log.info("Agent가 다음 단계로 전환 결정 - interviewId: {}, currentPhase: {}",
                interview.getId(), currentPhase);

        if (!interview.isLastPhase()) {
            InterviewPhase previousPhase = interview.getCurrentPhase();
            interview.nextPhase();
            InterviewPhase nextPhase = interview.getCurrentPhase();

            log.info("면접 단계 전환 완료 - interviewId: {}, 이전: {}, 다음: {}",
                    interview.getId(), previousPhase, nextPhase);

            // 다음 단계가 AI 생성 단계면 첫 질문 생성
            if (nextPhase != null && !nextPhase.isTemplate()) {
                questionService.generateFirstQuestionByAgent(interview, previousPhase, nextPhase);
            }
        }

        return AnswerResponse.from(savedAnswer);
    }

    /**
     * GENERATE_QUESTION 액션 처리 - 새 질문 생성
     */
    private AnswerResponse handleGenerateQuestion(
            Interview interview,
            InterviewPhase phase,
            Question currentQuestion,
            AiInterviewAgentResponse agentResponse,
            Answer savedAnswer
    ) {
        if (agentResponse.message() != null) {
            int nextSequence = questionRepository.countByInterviewId(interview.getId()) + 1;

            Question newQuestion = Question.builder()
                    .content(agentResponse.message())
                    .interview(interview)
                    .phase(phase)
                    .sequence(nextSequence)
                    .isFollowUp(true)
                    .parentQuestion(currentQuestion)
                    .build();

            Question saved = questionRepository.save(newQuestion);
            log.info("Agent가 새 질문 생성 - interviewId: {}, questionId: {}, phase: {}",
                    interview.getId(), saved.getId(), phase);

            return AnswerResponse.of(savedAnswer, QuestionResponse.from(saved));
        }

        return AnswerResponse.from(savedAnswer);
    }

    /**
     * Template 단계의 자동 전환 체크 (OPENING → TECHNICAL, CLOSING → 완료)
     * Template 단계에서는 모든 고정 질문에 답변이 완료되면 자동으로 다음 단계로 전환합니다.
     */
    private void checkAndMoveToNextPhaseForTemplate(Interview interview, InterviewPhase currentPhase) {
        List<Question> phaseQuestions = questionRepository
                .findByInterviewIdOrderBySequence(interview.getId())
                .stream()
                .filter(q -> q.getPhase() == currentPhase && !q.isFollowUp())
                .toList();

        boolean allAnswered = phaseQuestions.stream().allMatch(Question::isAnswered);

        if (allAnswered && !phaseQuestions.isEmpty()) {
            log.info("Template 단계 완료 - interviewId: {}, phase: {}", interview.getId(), currentPhase);

            if (!interview.isLastPhase() && interview.getCurrentPhase() == currentPhase) {
                InterviewPhase previousPhase = interview.getCurrentPhase();
                interview.nextPhase();
                InterviewPhase nextPhase = interview.getCurrentPhase();

                log.info("면접 단계 전환 - interviewId: {}, 이전: {}, 다음: {}",
                        interview.getId(), previousPhase, nextPhase);

                // 다음 단계가 AI 생성 단계면 첫 질문 생성
                if (nextPhase != null && !nextPhase.isTemplate()) {
                    questionService.generateFirstQuestionByAgent(interview, previousPhase, nextPhase);
                }
            }
        }
    }
}
