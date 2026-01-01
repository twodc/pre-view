package com.example.pre_view.domain.answer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.dto.AnswerCreateRequest;
import com.example.pre_view.domain.answer.dto.AnswerResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.answer.repository.AnswerRepository;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.service.AiInterviewService;
import com.example.pre_view.domain.question.dto.QuestionResponse;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;
import com.example.pre_view.domain.question.service.QuestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AiInterviewService aiInterviewService;
    private final QuestionService questionService;

    /**
     * 답변 생성 및 AI 피드백 처리
     * 
     * 트랜잭션 전략:
     * 1. AI 호출은 트랜잭션 밖에서 수행 (외부 API 호출로 인한 긴 대기 시간 방지)
     * 2. DB 저장은 별도 트랜잭션으로 수행 (AI 실패 시에도 답변은 저장)
     */
    public AnswerResponse createAnswer(Long questionId, AnswerCreateRequest request) {
        log.info("답변 생성 시작 - questionId: {}", questionId);

        // 1단계: 질문 조회 (interview까지 함께 조회 - N+1 문제 해결)
        Question question = questionRepository.findByIdWithInterview(questionId)
                .orElseThrow(() -> {
                    log.warn("질문을 찾을 수 없음 - questionId: {}", questionId);
                    return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
                });

        // 2단계: AI 피드백 생성 (트랜잭션 밖에서 수행 - 외부 API 호출)
        log.debug("AI 피드백 생성 시작 - questionId: {}, phase: {}", questionId, question.getPhase());
        AiFeedbackResponse aiFeedback = generateAiFeedbackSafely(
                question.getPhase(),
                question.getContent(),
                request.content());
        log.debug("AI 피드백 생성 완료 - questionId: {}, score: {}", questionId, aiFeedback.score());

        // 3단계: DB 저장 (별도 트랜잭션으로 수행)
        return saveAnswerWithFollowUp(question, request, aiFeedback);
    }

    /**
     * AI 피드백 생성 (트랜잭션 없이 수행)
     */
    private AiFeedbackResponse generateAiFeedbackSafely(InterviewPhase phase, String question, String answer) {
        return aiInterviewService.generateFeedback(phase, question, answer);
    }

    @Transactional
    private AnswerResponse saveAnswerWithFollowUp(Question question, AnswerCreateRequest request,
            AiFeedbackResponse aiFeedback) {
        Answer answer = Answer.builder()
                .question(question)
                .content(request.content())
                .feedback(aiFeedback.feedback())
                .score(aiFeedback.score())
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        question.markAsAnswered();
        log.info("답변 저장 완료 - questionId: {}, answerId: {}, score: {}",
                question.getId(), savedAnswer.getId(), savedAnswer.getScore());

        // Follow-up 질문 생성 조건 체크:
        // 1. 일반적으로 follow-up 질문에 대한 답변에는 follow-up을 생성하지 않음
        // 단, 기술 면접(TECHNICAL)의 follow-up 질문에 대해서는 1개까지 추가 follow-up 허용
        // 2. 이미 해당 질문에 대한 follow-up이 존재하면 생성하지 않음
        // 3. 점수가 너무 낮으면 (4점 이하) follow-up을 생성하지 않고 넘어감
        boolean allowFollowUpForFollowUp = question.isFollowUp() && question.getPhase() == InterviewPhase.TECHNICAL;
        boolean canCreateFollowUp = aiFeedback.needsFollowUp()
                && aiFeedback.followUpQuestion() != null
                && (allowFollowUpForFollowUp || !question.isFollowUp()) // 기술 면접의 follow-up 질문은 예외적으로 허용
                && questionRepository.findByParentQuestionId(question.getId()).isEmpty() // 이미 follow-up이 있으면 생성 안 함
                && aiFeedback.score() > 4; // 점수가 너무 낮으면 (4점 이하) follow-up 생성 안 함

        if (canCreateFollowUp) {
            log.debug("후속 질문 생성 시작 - questionId: {}, score: {}", question.getId(), aiFeedback.score());
            Question followUp = Question.builder()
                    .content(aiFeedback.followUpQuestion())
                    .interview(question.getInterview())
                    .phase(question.getPhase())
                    .sequence(getNextSequence(question.getInterview().getId()))
                    .parentQuestion(question)
                    .isFollowUp(true)
                    .build();

            Question savedFollowUp = questionRepository.save(followUp);
            log.info("후속 질문 생성 완료 - parentQuestionId: {}, followUpQuestionId: {}",
                    question.getId(), savedFollowUp.getId());

            return AnswerResponse.of(savedAnswer, QuestionResponse.from(savedFollowUp));
        } else {
            if (aiFeedback.needsFollowUp() && aiFeedback.followUpQuestion() != null) {
                log.debug(
                        "후속 질문 생성 스킵 - questionId: {}, 이유: 이미 follow-up 존재 또는 follow-up 질문에 대한 답변 또는 점수가 낮음 (score: {})",
                        question.getId(), aiFeedback.score());
            }
        }

        checkAndMoveToNextPhase(question.getInterview(), question.getPhase());

        return AnswerResponse.from(savedAnswer);
    }

    /**
     * 현재 단계의 모든 질문에 답변이 완료되었는지 확인하고, 완료되면 다음 단계로 전환
     * 다음 단계가 AI 질문 단계인 경우 자동으로 질문을 생성합니다.
     * 
     * @param interview    면접 엔티티
     * @param currentPhase 현재 면접 단계
     */
    private void checkAndMoveToNextPhase(Interview interview, InterviewPhase currentPhase) {
        // 현재 단계의 모든 질문 조회 (Follow-up 제외)
        List<Question> phaseQuestions = questionRepository
                .findByInterviewIdOrderBySequence(interview.getId())
                .stream()
                .filter(q -> q.getPhase() == currentPhase && !q.isFollowUp())
                .toList();

        boolean allAnswered = phaseQuestions.stream()
                .allMatch(Question::isAnswered);

        if (allAnswered && !phaseQuestions.isEmpty()) {
            log.info("현재 단계의 모든 질문 완료 - interviewId: {}, phase: {}, 질문 수: {}",
                    interview.getId(), currentPhase, phaseQuestions.size());

            // 다음 단계로 전환 (마지막 단계가 아니고, 현재 단계가 마지막 단계가 아닌 경우)
            if (!interview.isLastPhase() && interview.getCurrentPhase() == currentPhase) {
                InterviewPhase previousPhase = interview.getCurrentPhase();
                interview.nextPhase();
                InterviewPhase nextPhase = interview.getCurrentPhase();
                log.info("면접 단계 전환 완료 - interviewId: {}, 이전 단계: {}, 새 단계: {}",
                        interview.getId(), previousPhase, nextPhase);

                // 다음 단계가 AI 질문 단계이고 질문이 부족하면 자동으로 생성
                if (nextPhase != null && !nextPhase.isTemplate()) {
                    List<Question> allQuestions = questionRepository
                            .findByInterviewIdOrderBySequence(interview.getId());

                    // 다음 단계의 질문 수 확인 (Follow-up 제외)
                    long nextPhaseQuestionCount = allQuestions.stream()
                            .filter(q -> q.getPhase() == nextPhase && !q.isFollowUp())
                            .count();

                    // 필요한 질문 수 (defaultQuestionCount)
                    int requiredQuestionCount = nextPhase.getDefaultQuestionCount();

                    // 부족한 질문 수만큼 생성
                    if (nextPhaseQuestionCount < requiredQuestionCount) {
                        int questionsToGenerate = (int) (requiredQuestionCount - nextPhaseQuestionCount);
                        log.info("다음 단계 AI 질문 자동 생성 시작 - interviewId: {}, phase: {}, 필요: {}개, 현재: {}개, 생성: {}개",
                                interview.getId(), nextPhase, requiredQuestionCount, nextPhaseQuestionCount,
                                questionsToGenerate);

                        for (int i = 0; i < questionsToGenerate; i++) {
                            Question aiQuestion = questionService.generateAiQuestionInNewTransaction(
                                    interview, nextPhase, allQuestions);
                            if (aiQuestion != null) {
                                allQuestions.add(aiQuestion);
                                log.info("다음 단계 AI 질문 자동 생성 완료 - interviewId: {}, phase: {}, questionId: {}, {}/{}",
                                        interview.getId(), nextPhase, aiQuestion.getId(), i + 1, questionsToGenerate);
                            } else {
                                log.warn("다음 단계 AI 질문 생성 실패 - interviewId: {}, phase: {}, {}/{}",
                                        interview.getId(), nextPhase, i + 1, questionsToGenerate);
                                break; // 생성 실패 시 중단
                            }
                        }
                    }
                }
            }
        }
    }

    private int getNextSequence(Long interviewId) {
        return questionRepository.countByInterviewId(interviewId) + 1;
    }
}
