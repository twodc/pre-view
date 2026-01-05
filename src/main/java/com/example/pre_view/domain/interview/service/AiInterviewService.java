package com.example.pre_view.domain.interview.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.dto.AiInterviewAgentResponse;
import com.example.pre_view.domain.interview.dto.AiReportResponse;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

import io.github.resilience4j.retry.annotation.Retry;

import lombok.extern.slf4j.Slf4j;

import com.example.pre_view.common.service.PromptTemplateService;

@Slf4j
@Service
public class AiInterviewService {

    private final ChatClient chatClient;
    private final PromptTemplateService promptTemplateService;

    public AiInterviewService(ChatClient.Builder builder, PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
        this.chatClient = builder.build();
    }

    /**
     * 답변에 대한 AI 피드백 생성
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverFeedback")
    public AiFeedbackResponse generateFeedback(InterviewPhase phase, String question, String answer) {
        log.debug("AI 피드백 생성 시작 - phase: {}", phase);

        // PromptTemplateService에서 단계별 통합 프롬프트 로드
        String userPrompt = promptTemplateService.buildFeedbackUserPrompt(phase, question, answer);

        return chatClient.prompt()
                .system(promptTemplateService.getFeedbackSystemPrompt())
                .user(userPrompt)
                .call()
                .entity(AiFeedbackResponse.class);
    }

    /**
     * generateFeedback의 Fallback 메서드
     * 모든 재시도 실패 시 호출됨
     */
    public AiFeedbackResponse recoverFeedback(InterviewPhase phase, String question, String answer, Exception e) {
        log.error("AI 피드백 생성 실패 (모든 재시도 실패) - phase: {}, 기본 피드백 반환", phase, e);
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
        log.debug("AI 리포트 생성 시작 - context: {}", context);

        String qnaContent = answers.stream()
                .map(a -> String.format("[%s] Question: %s\nAnswer: %s\nScore: %d",
                        a.getQuestion().getPhase().getDescription(),
                        a.getQuestion().getContent(),
                        a.getContent(),
                        a.getScore()))
                .collect(Collectors.joining("\n\n"));

        String userPrompt = promptTemplateService.buildReportUserPrompt(context, qnaContent);

        return chatClient.prompt()
                .system(promptTemplateService.getFeedbackSystemPrompt())
                .user(userPrompt)
                .call()
                .entity(AiReportResponse.class);
    }

    /**
     * generateReport의 Fallback 메서드
     * 모든 재시도 실패 시 호출됨
     */
    public AiReportResponse recoverReport(String context, List<Answer> answers, Exception e) {
        log.error("AI 리포트 생성 실패 (모든 재시도 실패) - context: {}, Fallback 리포트 반환", context, e);
        return new AiReportResponse(
                "AI 서비스 연결 문제로 리포트를 생성할 수 없습니다. 잠시 후 다시 시도해주세요.",
                List.of(),
                List.of(),
                List.of(),
                0);
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

        log.debug("AI 면접 에이전트 단계 처리 시작 - phase: {}, followUpCount: {}", phase, currentTopicFollowUpCount);

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

        AiInterviewAgentResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(AiInterviewAgentResponse.class);

        log.info("AI 면접 에이전트 응답 생성 완료 - phase: {}, action: {}, message: {}",
                phase, response.action(), response.message() != null ? "있음" : "없음");

        return response;
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

        return null;
    }
}
