package com.example.pre_view.domain.answer.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.dto.AnswerCreateRequest;
import com.example.pre_view.domain.answer.dto.AnswerResponse;
import com.example.pre_view.domain.interview.dto.AiInterviewAgentResponse;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.InterviewAction;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.repository.InterviewRepository;
import com.example.pre_view.domain.interview.service.AiInterviewService;
import com.example.pre_view.domain.question.entity.Question;
import com.example.pre_view.domain.question.repository.QuestionRepository;
import com.example.pre_view.domain.question.service.QuestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 답변 생성 흐름을 조율하는 Facade 서비스
 *
 * 역할:
 * - 비즈니스 흐름 조율 (AI 호출 → DB 저장)
 * - 트랜잭션 없이 외부 API 호출 수행
 * - 실제 DB 작업은 AnswerService에 위임
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerFacade {

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AiInterviewService aiInterviewService;
    private final AnswerService answerService;
    private final QuestionService questionService;

    /**
     * 답변 생성 및 처리
     * 트랜잭션 없이 AI 호출을 먼저 수행하고, DB 저장은 별도 트랜잭션에서 처리
     *
     * @param interviewId 면접 ID (보안 검증용)
     * @param questionId 질문 ID
     * @param memberId 현재 사용자 ID (권한 검증용)
     * @param request 답변 생성 요청
     * @return 답변 응답 DTO
     */
    public AnswerResponse createAnswer(Long interviewId, Long questionId, Long memberId, AnswerCreateRequest request) {
        log.info("답변 생성 시작 - interviewId: {}, questionId: {}, memberId: {}", interviewId, questionId, memberId);

        // 1. 면접 권한 검증
        Interview interview = interviewRepository.findByIdAndMemberIdAndDeletedFalse(interviewId, memberId)
                .orElseThrow(() -> {
                    boolean exists = interviewRepository.findByIdAndDeletedFalse(interviewId).isPresent();
                    if (exists) {
                        log.warn("면접 접근 권한 없음 - interviewId: {}, memberId: {}", interviewId, memberId);
                        return new BusinessException(ErrorCode.ACCESS_DENIED);
                    }
                    log.warn("면접을 찾을 수 없음 - interviewId: {}", interviewId);
                    return new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
                });

        // 2. 질문 조회 및 검증
        Question question = questionRepository.findByIdWithInterview(questionId)
                .orElseThrow(() -> {
                    log.warn("질문을 찾을 수 없음 - questionId: {}", questionId);
                    return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
                });

        // 보안 검증: 질문이 해당 면접에 속하는지 확인
        if (!question.getInterview().getId().equals(interviewId)) {
            log.warn("면접 ID 불일치 - 요청된 interviewId: {}, 실제 interviewId: {}",
                    interviewId, question.getInterview().getId());
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        InterviewPhase phase = question.getPhase();

        // 3. AI 피드백 생성 (트랜잭션 밖)
        log.debug("AI 피드백 생성 시작 - questionId: {}, phase: {}", questionId, phase);
        AiFeedbackResponse aiFeedback = aiInterviewService.generateFeedback(
                phase,
                question.getContent(),
                request.content());
        log.debug("AI 피드백 생성 완료 - questionId: {}, score: {}", questionId, aiFeedback.score());

        // 4. Template 단계는 Agent 호출 없이 바로 저장
        if (phase.isTemplate()) {
            log.debug("Template 단계 - Agent 호출 생략, 바로 저장 - phase: {}", phase);
            return answerService.saveAnswerForTemplate(question, request.content(), aiFeedback);
        }

        // 5. AI 생성 단계(TECHNICAL, PERSONALITY)는 Agent 호출 (트랜잭션 밖)
        int followUpDepth = questionService.calculateFollowUpDepth(question);

        // Follow-up 질문 제한 (최대 2회) - AI가 제한을 어길 수 있으므로 서버에서 강제
        if (followUpDepth >= 2) {
            log.info("Follow-up 질문 제한 도달 - questionId: {}, depth: {}, 강제로 다음 단계 전환",
                    questionId, followUpDepth);
            AiInterviewAgentResponse forcedResponse = new AiInterviewAgentResponse(
                    "꼬리 질문 제한에 도달하여 다음 단계로 넘어갑니다.",
                    InterviewAction.NEXT_PHASE,
                    null,
                    null
            );
            return answerService.saveAnswerWithAgentResult(question, request.content(), aiFeedback, forcedResponse);
        }

        // 이전 질문-답변 히스토리 수집
        List<String> previousQuestions = new ArrayList<>(
                questionService.getPreviousQuestions(interview, phase));
        List<String> previousAnswers = new ArrayList<>(
                questionService.getPreviousAnswers(interview, phase));

        // 현재 질문과 답변도 히스토리에 추가
        previousQuestions.add(question.getContent());
        previousAnswers.add(request.content());

        log.debug("Agent 호출 시작 - phase: {}, followUpDepth: {}, historySize: {}",
                phase, followUpDepth, previousQuestions.size());

        AiInterviewAgentResponse agentResponse = aiInterviewService.processInterviewStep(
                phase,
                null,  // bridgeAnswer는 첫 질문에만 사용
                interview.buildContext(),
                interview.getResumeText(),
                interview.getPortfolioText(),
                previousQuestions,
                previousAnswers,
                followUpDepth
        );

        // Agent 응답이 null인 경우 (AI 호출 실패 시) 기본 응답 생성
        if (agentResponse == null) {
            log.warn("Agent 응답이 null - AI 호출 실패, NEXT_PHASE로 처리 - phase: {}", phase);
            agentResponse = new AiInterviewAgentResponse(
                    "AI 서비스 연결 문제로 판단을 수행할 수 없었습니다.",
                    InterviewAction.NEXT_PHASE,
                    null,
                    null
            );
        }

        log.info("Agent 호출 완료 - action: {}, hasMessage: {}",
                agentResponse.action(), agentResponse.message() != null);

        // 6. DB 저장 및 Agent 결과 처리 (트랜잭션 안)
        return answerService.saveAnswerWithAgentResult(
                question,
                request.content(),
                aiFeedback,
                agentResponse
        );
    }
}
