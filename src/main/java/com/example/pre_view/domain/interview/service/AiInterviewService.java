package com.example.pre_view.domain.interview.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.pre_view.common.llm.GroqChatService;
import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.dto.AiInterviewAgentResponse;
import com.example.pre_view.domain.interview.dto.AiReportResponse;
import com.example.pre_view.domain.interview.enums.InterviewAction;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

import com.example.pre_view.common.service.PromptTemplateService;
import com.example.pre_view.domain.llm.config.LlmConfig;
import com.example.pre_view.domain.llm.dto.LlmFeedbackResponse;
import com.example.pre_view.domain.llm.dto.LlmInterviewResponse;
import com.example.pre_view.domain.llm.dto.LlmReportResponse;
import com.example.pre_view.domain.llm.service.LlmService;

/**
 * AI 면접 서비스
 *
 * OpenAI API 또는 LLM 서비스를 통해 면접 관련 AI 기능을 제공합니다.
 * - 피드백 생성: 답변에 대한 AI 평가
 * - 리포트 생성: 면접 종합 분석
 * - 면접 에이전트: 실시간 질문 생성 및 진행
 *
 * llm.enabled=true인 경우 Python LLM 서비스(llm-service:8003) 사용
 * llm.enabled=false인 경우 Groq API 사용
 */
@Slf4j
@Service
public class AiInterviewService {

    private final GroqChatService groqChatService;
    private final PromptTemplateService promptTemplateService;
    private final LlmService llmService;
    private final LlmConfig llmConfig;

    // 메트릭
    private final Timer feedbackTimer;
    private final Timer reportTimer;
    private final Timer interviewStepTimer;
    private final Counter aiCallSuccessCounter;
    private final Counter aiCallFailureCounter;

    public AiInterviewService(GroqChatService groqChatService, PromptTemplateService promptTemplateService,
            LlmService llmService, LlmConfig llmConfig, MeterRegistry meterRegistry) {
        this.promptTemplateService = promptTemplateService;
        this.groqChatService = groqChatService;
        this.llmService = llmService;
        this.llmConfig = llmConfig;

        // AI 호출 응답 시간 메트릭
        this.feedbackTimer = Timer.builder("ai.feedback.duration")
                .description("AI 피드백 생성 응답 시간")
                .register(meterRegistry);
        this.reportTimer = Timer.builder("ai.report.duration")
                .description("AI 리포트 생성 응답 시간")
                .register(meterRegistry);
        this.interviewStepTimer = Timer.builder("ai.interview.step.duration")
                .description("AI 면접 에이전트 단계별 응답 시간")
                .register(meterRegistry);

        // AI 호출 성공/실패 카운터
        this.aiCallSuccessCounter = Counter.builder("ai.calls.success")
                .description("AI API 호출 성공 횟수")
                .register(meterRegistry);
        this.aiCallFailureCounter = Counter.builder("ai.calls.failure")
                .description("AI API 호출 실패 횟수")
                .register(meterRegistry);
    }

    /**
     * 답변에 대한 AI 피드백 생성
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverFeedback")
    public AiFeedbackResponse generateFeedback(InterviewPhase phase, String question, String answer) {
        log.debug("AI 피드백 생성 시작 - phase: {}, llmEnabled: {}", phase, llmConfig.isEnabled());

        // LLM 서비스 사용 여부 확인
        if (llmConfig.isEnabled()) {
            return generateFeedbackWithLlm(phase, question, answer);
        }

        // Groq API 사용 (Failover 지원: Qwen → Llama)
        return feedbackTimer.record(() -> {
            String systemPrompt = promptTemplateService.getFeedbackSystemPrompt();
            String userPrompt = promptTemplateService.buildFeedbackUserPrompt(phase, question, answer);

            AiFeedbackResponse response = groqChatService.chatCompletion(
                    systemPrompt, userPrompt, AiFeedbackResponse.class);

            aiCallSuccessCounter.increment();
            return response;
        });
    }

    /**
     * LLM 서비스를 사용한 피드백 생성
     */
    private AiFeedbackResponse generateFeedbackWithLlm(InterviewPhase phase, String question, String answer) {
        String systemPrompt = promptTemplateService.getFeedbackSystemPrompt();
        String userPrompt = promptTemplateService.buildFeedbackUserPrompt(phase, question, answer);

        LlmFeedbackResponse llmResponse = llmService.generateFeedback(systemPrompt, userPrompt);

        log.info("LLM 피드백 생성 완료 - score: {}", llmResponse.score());
        return new AiFeedbackResponse(
                llmResponse.feedback(),
                llmResponse.score(),
                llmResponse.isPassed(),
                llmResponse.improvementSuggestion());
    }

    /**
     * generateFeedback의 Fallback 메서드
     * 모든 재시도 실패 시 호출됨
     */
    public AiFeedbackResponse recoverFeedback(InterviewPhase phase, String question, String answer, Exception e) {
        log.error("AI 피드백 생성 실패 (모든 재시도 실패) - phase: {}, 기본 피드백 반환", phase, e);
        aiCallFailureCounter.increment();
        return new AiFeedbackResponse(
                "AI 서비스 연결 문제로 자동 피드백을 생성할 수 없었습니다. " +
                        "답변은 저장되었으며, 잠시 후 다시 시도해주세요.",
                5,
                false,
                null);
    }

    /**
     * 면접 종합 리포트 생성
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverReport")
    public AiReportResponse generateReport(String context, List<Answer> answers) {
        log.debug("AI 리포트 생성 시작 - context: {}, llmEnabled: {}", context, llmConfig.isEnabled());

        // LLM 서비스 사용 여부 확인
        if (llmConfig.isEnabled()) {
            return generateReportWithLlm(context, answers);
        }

        // Groq API 사용 (Failover 지원: Qwen → Llama)
        return reportTimer.record(() -> {
            String qnaContent = answers.stream()
                    .map(a -> String.format("[%s] Question: %s\nAnswer: %s\nScore: %d",
                            a.getQuestion().getPhase().getDescription(),
                            a.getQuestion().getContent(),
                            a.getContent(),
                            a.getScore()))
                    .collect(Collectors.joining("\n\n"));

            String systemPrompt = promptTemplateService.getFeedbackSystemPrompt();
            String userPrompt = promptTemplateService.buildReportUserPrompt(context, qnaContent);

            AiReportResponse response = groqChatService.chatCompletion(
                    systemPrompt, userPrompt, AiReportResponse.class);

            aiCallSuccessCounter.increment();
            return response;
        });
    }

    /**
     * LLM 서비스를 사용한 리포트 생성
     */
    private AiReportResponse generateReportWithLlm(String context, List<Answer> answers) {
        String qnaContent = answers.stream()
                .map(a -> String.format("[%s] Question: %s\nAnswer: %s\nScore: %d",
                        a.getQuestion().getPhase().getDescription(),
                        a.getQuestion().getContent(),
                        a.getContent(),
                        a.getScore()))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = promptTemplateService.getFeedbackSystemPrompt();
        String userPrompt = promptTemplateService.buildReportUserPrompt(context, qnaContent);

        LlmReportResponse llmResponse = llmService.generateReport(systemPrompt, userPrompt);

        log.info("LLM 리포트 생성 완료 - overallScore: {}", llmResponse.overallScore());
        return new AiReportResponse(
                llmResponse.summary(),
                llmResponse.strengths(),
                llmResponse.improvements(),
                llmResponse.recommendations(),
                llmResponse.overallScore(),
                List.of());  // questionFeedbacks는 별도 매핑 필요 시 추가
    }

    /**
     * generateReport의 Fallback 메서드
     * 모든 재시도 실패 시 호출됨
     */
    public AiReportResponse recoverReport(String context, List<Answer> answers, Exception e) {
        log.error("AI 리포트 생성 실패 (모든 재시도 실패) - context: {}, Fallback 리포트 반환", context, e);
        aiCallFailureCounter.increment();
        return new AiReportResponse(
                "AI 서비스 연결 문제로 리포트를 생성할 수 없습니다. 잠시 후 다시 시도해주세요.",
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of());  // questionFeedbacks - 빈 리스트
    }

    /**
     * AI 면접 에이전트의 단계별 처리 메서드
     * 
     * Technical/Behavioral 단계에서 사용자의 답변을 평가하고, 다음 질문을 생성할지 또는 다음 단계로 넘어갈지 판단합니다.
     * 
     * @param phase                     현재 면접 단계 (TECHNICAL 또는 PERSONALITY)
     * @param previousAnswer            브릿지 질문(Opening 단계의 마지막 질문)에 대한 답변 (nullable)
     * @param interviewContext          면접 컨텍스트 (포지션, 레벨, 기술스택 등)
     * @param resumeText                이력서 텍스트 (nullable)
     * @param portfolioText             포트폴리오 텍스트 (nullable)
     * @param previousQuestions         이전 질문 목록 (현재 단계의 질문들)
     * @param previousAnswers           이전 답변 목록 (현재 단계의 답변들, previousQuestions와 순서
     *                                  매칭)
     * @param currentTopicFollowUpCount 현재 주제에 대한 꼬리 질문 횟수 (최대 2회 제한)
     * @return AI 에이전트의 응답 (thought, action, message, evaluation)
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverInterviewStep")
    public AiInterviewAgentResponse processInterviewStep(
            InterviewPhase phase,
            String previousAnswer,
            String interviewContext,
            String resumeText,
            String portfolioText,
            List<String> previousQuestions,
            List<String> previousAnswers,
            int currentTopicFollowUpCount) {

        log.debug("AI 면접 에이전트 단계 처리 시작 - phase: {}, followUpCount: {}, llmEnabled: {}",
                phase, currentTopicFollowUpCount, llmConfig.isEnabled());

        // LLM 서비스 사용 여부 확인
        if (llmConfig.isEnabled()) {
            return processInterviewStepWithLlm(phase, previousAnswer, interviewContext, resumeText,
                    portfolioText, previousQuestions, previousAnswers, currentTopicFollowUpCount);
        }

        // 기존 Groq API 사용
        return interviewStepTimer.record(() -> {
            String systemPrompt = promptTemplateService.getInterviewAgentSystemPrompt(phase);

            String userPrompt;
            if (previousQuestions == null || previousQuestions.isEmpty()) {
                // 첫 질문 생성
                userPrompt = promptTemplateService.buildInterviewAgentFirstQuestionPrompt(
                        previousAnswer,
                        interviewContext,
                        resumeText,
                        portfolioText,
                        currentTopicFollowUpCount);
            } else {
                // 면접 진행 중
                userPrompt = promptTemplateService.buildInterviewAgentContinuePrompt(
                        interviewContext,
                        resumeText,
                        portfolioText,
                        previousQuestions,
                        previousAnswers,
                        currentTopicFollowUpCount);
            }

            AiInterviewAgentResponse response = groqChatService.chatCompletion(
                    systemPrompt, userPrompt, AiInterviewAgentResponse.class);

            log.info("AI 면접 에이전트 응답 생성 완료 - phase: {}, action: {}, message: {}",
                    phase, response.action(), response.message() != null ? "있음" : "없음");

            aiCallSuccessCounter.increment();
            return response;
        });
    }

    /**
     * LLM 서비스를 사용한 면접 에이전트 처리
     */
    private AiInterviewAgentResponse processInterviewStepWithLlm(
            InterviewPhase phase,
            String previousAnswer,
            String interviewContext,
            String resumeText,
            String portfolioText,
            List<String> previousQuestions,
            List<String> previousAnswers,
            int currentTopicFollowUpCount) {

        String systemPrompt = promptTemplateService.getInterviewAgentSystemPrompt(phase);

        String userPrompt;
        if (previousQuestions == null || previousQuestions.isEmpty()) {
            // 첫 질문 생성
            userPrompt = promptTemplateService.buildInterviewAgentFirstQuestionPrompt(
                    previousAnswer,
                    interviewContext,
                    resumeText,
                    portfolioText,
                    currentTopicFollowUpCount);
        } else {
            // 면접 진행 중
            userPrompt = promptTemplateService.buildInterviewAgentContinuePrompt(
                    interviewContext,
                    resumeText,
                    portfolioText,
                    previousQuestions,
                    previousAnswers,
                    currentTopicFollowUpCount);
        }

        LlmInterviewResponse llmResponse = llmService.processInterviewStep(systemPrompt, userPrompt);

        if (llmResponse == null) {
            // LLM 서비스 실패 시 null 반환 (QuestionService에서 Fallback 처리)
            return null;
        }

        log.info("LLM 면접 에이전트 응답 생성 완료 - phase: {}, action: {}, message: {}",
                phase, llmResponse.action(), llmResponse.message() != null ? "있음" : "없음");

        // LlmInterviewResponse -> AiInterviewAgentResponse 변환
        // action 문자열을 InterviewAction enum으로 변환
        InterviewAction action = parseInterviewAction(llmResponse.action());

        return new AiInterviewAgentResponse(
                llmResponse.thought(),
                action,
                llmResponse.message(),
                llmResponse.evaluation());
    }

    /**
     * LLM 응답의 action 문자열을 InterviewAction enum으로 변환
     */
    private InterviewAction parseInterviewAction(String actionStr) {
        if (actionStr == null) {
            return InterviewAction.NEXT_PHASE;
        }
        return switch (actionStr.toUpperCase()) {
            case "FOLLOW_UP", "NEW_TOPIC", "GENERATE_QUESTION" -> InterviewAction.GENERATE_QUESTION;
            case "NEXT_PHASE" -> InterviewAction.NEXT_PHASE;
            default -> InterviewAction.NEXT_PHASE;
        };
    }

    /**
     * processInterviewStep의 Fallback 메서드
     *
     * AI 호출 실패 시 null을 반환하고, QuestionService에서 Fallback 질문을 생성합니다.
     * Fallback 질문 생성은 QuestionService.generateFallbackQuestion()에서 전담합니다.
     */
    public AiInterviewAgentResponse recoverInterviewStep(
            InterviewPhase phase,
            String previousAnswer,
            String interviewContext,
            String resumeText,
            String portfolioText,
            List<String> previousQuestions,
            List<String> previousAnswers,
            int currentTopicFollowUpCount,
            Exception e) {
        log.error("AI 면접 에이전트 처리 실패 (모든 재시도 실패) - phase: {}, null 반환 (QuestionService에서 Fallback 처리)", phase, e);
        aiCallFailureCounter.increment();
        return null;
    }
}
