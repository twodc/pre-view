package com.example.pre_view.domain.interview.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.dto.AiInterviewAgentResponse;
import com.example.pre_view.domain.interview.dto.AiReportResponse;
import com.example.pre_view.domain.interview.enums.InterviewAction;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

import io.github.resilience4j.retry.annotation.Retry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiInterviewService {

    private final ChatClient chatClient;
    private final ChatClient.Builder chatClientBuilder;

    private static final String SYSTEM_PROMPT = """
            당신은 전문 면접관입니다.

            응답 형식:
            - JSON 형식으로만 응답하세요 (마크다운 코드 블록 없이).
            - 구체적이고 건설적인 피드백을 제공하세요.
            - 강점, 약점, 개선 제안을 포함한 상세한 피드백을 5-7문장으로 작성하세요.
            """;

    public AiInterviewService(ChatClient.Builder builder) {
        this.chatClientBuilder = builder;
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /**
     * 답변에 대한 AI 피드백 생성
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverFeedback")
    public AiFeedbackResponse generateFeedback(InterviewPhase phase, String question, String answer) {
        String evaluationCriteria = getEvaluationCriteria(phase);
        boolean allowFollowUp = phase.isAllowFollowUp();

        String phaseSpecificInstruction = getPhaseSpecificInstruction(phase);

        String followUpInstruction = allowFollowUp
                ? """
                    후속 질문 생성 규칙:
                    - 답변이 적절히 좋은 경우(점수 >= 5)이지만 약간의 설명이나 추가 세부사항이 필요한 경우에만 후속 질문을 생성하세요.
                    - 다음 경우에는 후속 질문을 생성하지 마세요:
                      * 점수가 4 이하인 경우 (지원자가 질문에 어려움을 겪은 경우 - 낮은 점수를 주고 넘어가세요)
                      * 답변이 완전히 틀렸거나 근본적인 오해를 보여주는 경우
                      * 답변이 너무 모호하거나 짧아서 평가하기 어려운 경우
                    - 후속 질문은 추가 세부사항이나 예시를 요청해야 하며, 같은 질문을 다른 말로 반복하지 마세요.
                    - 각 주 질문은 최대 하나의 후속 질문만 가질 수 있습니다.
                    - 후속 질문은 원래 질문과 다르게 작성해야 합니다 (유사한 표현을 피하세요).

                    위 조건이 충족되면 needsFollowUp을 true로 설정하고 후속 질문을 제공하세요.
                    그렇지 않으면 needsFollowUp을 false로 설정하고 followUpQuestion을 null로 설정하세요.

                    좋은 후속 질문 예시:
                    - "해당 경험에서 가장 어려웠던 부분은 무엇이었나요?"
                    - "이 기술을 실제 프로젝트에 어떻게 적용하셨는지 구체적인 예시를 들어주실 수 있나요?"
                    - "이런 상황에서 발생할 수 있는 문제점이나 트레이드오프는 무엇이라고 생각하시나요?"
                    """
                : "needsFollowUp을 false로 설정하고 followUpQuestion을 null로 설정하세요.";

        // 파라미터가 많을 경우 Map으로 그룹화하여 .params() 사용
        Map<String, Object> params = new HashMap<>();
        params.put("phase", phase.getDescription());
        params.put("question", question);
        params.put("answer", answer);
        params.put("criteria", evaluationCriteria);
        params.put("phaseInstruction", phaseSpecificInstruction);
        params.put("followUpInstruction", followUpInstruction);

        String feedbackGuideline = getFeedbackGuideline(phase);

        String feedbackExample = getFeedbackExample(phase);

        return chatClient.prompt()
                .user(u -> u
                        .text("""
                            면접 단계에 따라 지원자의 답변을 평가하세요.

                            면접 단계: {phase}
                            질문: {question}
                            답변: {answer}

                            평가 기준: {criteria}

                            {phaseInstruction}

                            {feedbackGuideline}

                            {followUpInstruction}

                            피드백 구조 요구사항:
                            - 각 평가 기준을 구체적으로 다루세요
                            - 다음을 포함하세요: (1) 강점 (좋았던 점), (2) 개선 영역 (부족했던 점), (3) 구체적인 개선 제안

                            피드백 예시:
                            {feedbackExample}

                            응답 형식 (JSON만, 마크다운 없이):
                            {{
                                "feedback": "구체적이고 상세한 피드백 (5-7문장)",
                                "score": 7,
                                "needsFollowUp": false,
                                "followUpQuestion": null
                            }}

                            점수는 1부터 10 사이여야 합니다.
                            """)
                        .params(params)
                        .param("feedbackGuideline", feedbackGuideline)
                        .param("feedbackExample", feedbackExample))
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

    // 평가 기준 반환
    private String getEvaluationCriteria(InterviewPhase phase) {
        return switch (phase) {
            case OPENING -> "자연스러움, 예의, 긍정적인 태도, 명확성, 관련 경험 언급, 지원 동기의 진정성, 논리적 구성";
            case PERSONALITY -> "구체적인 사례(STAR 기법), 자기 인식, 협업 능력, 문제 해결 접근법, 성장 의지";
            case TECHNICAL -> "기술적 정확성, 깊이 있는 이해, 실무 적용 가능성, 문제 해결 능력";
            case CLOSING -> "적극성, 회사/직무에 대한 관심, 준비성";
        };
    }

    /**
     * 단계별 구체적인 지시사항
     */
    private String getPhaseSpecificInstruction(InterviewPhase phase) {
        return switch (phase) {
            case OPENING ->
                """
                    중요: 이것은 OPENING 단계(인사 및 자기소개)입니다.
                    지원자의 인사, 예의, 태도, 첫인상, 배경, 동기, 경력 목표에 집중하세요.
                    기술 질문을 하거나 기술적 세부사항을 파고들지 마세요.
                    기술 질문은 TECHNICAL 단계에서 질문할 것입니다.
                    """;
            case PERSONALITY ->
                """
                    중요: 이것은 PERSONALITY/BEHAVIORAL 단계입니다.
                    지원자의 협업 능력, 갈등 해결, 리더십 및 기타 인성 측면에 집중하세요.
                    STAR 기법(Situation, Task, Action, Result)을 구체적인 예시와 함께 사용하는지 평가하세요.
                    """;
            case TECHNICAL ->
                """
                    중요: 이것은 TECHNICAL 단계입니다.
                    지원자의 기술 지식, 이해력, 실무 적용 능력을 평가하세요.
                    기술적 정확성과 깊이 있는 이해에 집중하세요.
                    """;
            case CLOSING ->
                """
                    중요: 이것은 CLOSING 단계입니다.
                    지원자의 적극성, 회사/직무에 대한 관심, 준비성에 집중하세요.
                    """;
        };
    }

    /**
     * 단계별 피드백 가이드라인
     */
    private String getFeedbackGuideline(InterviewPhase phase) {
        return switch (phase) {
            case OPENING -> """
                    피드백 작성 가이드라인:
                    - 인사가 자연스럽고 예의 바른지 평가하세요
                    - 긍정적인 태도와 자신감 있는 시작이 있었는지 언급하세요
                    - 자기소개가 명확하고 논리적으로 구성되었는지 평가하세요
                    - 관련 경험과 동기가 구체적으로 언급되었는지 확인하세요
                    - 첫인상에 대한 구체적인 평가를 제공하세요
                    - 인사, 자기소개, 전체적인 프레젠테이션에 대한 개선 제안을 하세요
                    """;
            case PERSONALITY -> """
                    피드백 작성 가이드라인:
                    - STAR 기법을 사용한 구체적인 예시가 제공되었는지 평가하세요
                    - 자기 인식과 성장 마인드셋을 평가하세요
                    - 협업 능력과 문제 해결 접근법을 평가하세요
                    - 개선이 필요한 영역에 대한 구체적인 조언을 제공하세요
                    """;
            case TECHNICAL -> """
                    피드백 작성 가이드라인:
                    - 기술적 정확성과 이해의 깊이를 평가하세요
                    - 실무 적용 가능성을 평가하세요
                    - 문제 해결 능력과 접근법을 평가하세요
                    - 부족한 영역에 대한 구체적인 학습 방향을 제안하세요
                    """;
            case CLOSING -> """
                    피드백 작성 가이드라인:
                    - 적극성과 관심 수준을 평가하세요
                    - 회사/직무에 대한 이해와 준비성을 평가하세요
                    - 마무리가 인상적인지 평가하세요
                    - 추가로 준비할 사항을 제안하세요
                    """;
        };
    }

    /**
     * 단계별 피드백 예시 (Few-shot learning을 위한 한국어 예시)
     */
    private String getFeedbackExample(InterviewPhase phase) {
        return switch (phase) {
            case OPENING ->
                """
                        "인사말이 자연스럽고 예의 바른 태도가 좋았으며, 자기소개가 명확하고 논리적으로 구성되어 있어 이해하기 쉬웠습니다. 긍정적인 첫인상을 주었고, 특히 관련 경험을 구체적으로 언급하여 지원 동기의 진정성을 잘 보여주었습니다. 다만, 조금 더 자신감 있는 어조로 시작하고, 지원 동기를 설명할 때 더 구체적인 사례나 데이터를 활용한다면 설득력이 더욱 높아질 것입니다. 또한 자신의 강점을 단순 나열이 아닌 스토리텔링 방식으로 전달하면 더욱 효과적일 것입니다."
                        """;
            case PERSONALITY ->
                """
                        "STAR 기법을 활용한 구체적인 사례 제시가 인상적이었습니다. 상황, 과제, 행동, 결과를 명확하게 구분하여 설명하여 문제 해결 능력을 잘 보여주었습니다. 자기 인식 능력도 뛰어나 보이며, 성장 의지가 느껴집니다. 다만, 협업 관련 질문에서 팀원들의 기여도도 함께 언급한다면 더욱 좋을 것입니다. 앞으로는 자신의 역할뿐만 아니라 팀 전체의 성과에 대한 관점도 보여주시면 더욱 좋은 평가를 받을 수 있을 것입니다."
                        """;
            case TECHNICAL ->
                """
                        "기술적 정확성과 깊이 있는 이해도가 우수합니다. 핵심 개념을 정확하게 설명하고, 실무 적용 가능성까지 고려한 답변이 인상적이었습니다. 문제 해결 접근법도 체계적이고 논리적입니다. 다만, 특정 기술 스택에 대한 더 깊은 이해나 실제 경험 사례를 추가로 언급한다면 더욱 강한 인상을 줄 수 있을 것입니다. 앞으로는 기술 선택의 배경과 트레이드오프를 설명할 수 있는 능력을 기르시면 좋겠습니다."
                        """;
            case CLOSING ->
                """
                        "회사와 직무에 대한 깊은 관심과 준비성이 잘 드러났습니다. 적극적인 태도와 구체적인 질문이 인상적이었으며, 면접을 마무리하는 방식이 전문적이었습니다. 다만, 자신의 입사 후 기여 방안을 더 구체적으로 언급한다면 더욱 좋을 것입니다. 앞으로는 회사의 최근 뉴스나 프로젝트에 대한 이해를 바탕으로 한 추가 질문을 준비하시면 면접관에게 더 긍정적인 인상을 줄 수 있을 것입니다."
                        """;
        };
    }

    /**
     * AI 서비스 오류 시 사용할 기본 질문
     */
    private String getFallbackQuestion(InterviewPhase phase) {
        return switch (phase) {
            case PERSONALITY -> "팀에서 갈등이 발생했을 때 어떻게 해결하시겠어요?";
            case TECHNICAL -> "가장 자신 있는 기술 스택에 대해 설명해주세요.";
            default -> phase.getDescription() + " 관련 질문";
        };
    }

    /**
     * 면접 종합 리포트 생성
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverReport")
    public AiReportResponse generateReport(String context, List<Answer> answers) {
        String qnaContent = answers.stream()
                .map(a -> String.format("[%s] Question: %s\nAnswer: %s\nScore: %d",
                        a.getQuestion().getPhase().getDescription(),
                        a.getQuestion().getContent(),
                        a.getContent(),
                        a.getScore()))
                .collect(Collectors.joining("\n\n"));

        return chatClient.prompt()
                .user(u -> u
                        .text("""
                            지원자의 성과를 분석하는 종합 면접 리포트를 작성하세요.

                            포지션: {position}

                            면접 단계별 질문, 답변, 점수:

                            {qnaContent}

                            응답 형식 (JSON만, 마크다운 없이):
                            {{
                                "summary": "종합 평가 (2-3문장)",
                                "strengths": ["강점1", "강점2"],
                                "improvements": ["개선점1", "개선점2"],
                                "recommendedTopics": ["추천 학습 주제1", "추천 학습 주제2"],
                                "overallScore": 7
                            }}

                            overallScore는 1부터 10 사이여야 하며, 모든 답변을 고려한 평균으로 계산하세요.
                            """)
                        .param("position", context)
                        .param("qnaContent", qnaContent))
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

        String systemPrompt = buildInterviewAgentSystemPrompt(phase);
        String userPrompt = buildInterviewAgentUserPrompt(
                phase,
                previousAnswer,
                interviewContext,
                resumeText,
                portfolioText,
                previousQuestions,
                previousAnswers,
                currentTopicFollowUpCount);

        // 별도의 ChatClient 인스턴스를 사용하여 System Prompt를 동적으로 설정
        ChatClient agentChatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .build();

        AiInterviewAgentResponse response = agentChatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(AiInterviewAgentResponse.class);

        log.info("AI 면접 에이전트 응답 생성 완료 - phase: {}, action: {}, message: {}",
                phase, response.action(), response.message() != null ? "있음" : "없음");

        return response;
    }

    /**
     * processInterviewStep의 Fallback 메서드
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
        log.error("AI 면접 에이전트 처리 실패 (모든 재시도 실패) - phase: {}, Fallback 응답 반환", phase, e);

        // Fallback: 기본 질문 생성 액션 반환
        String fallbackQuestion = getFallbackQuestion(phase);
        return new AiInterviewAgentResponse(
                "AI 서비스 연결 문제로 판단을 수행할 수 없었습니다. 기본 질문을 생성합니다.",
                InterviewAction.GENERATE_QUESTION,
                fallbackQuestion,
                null);
    }

    /**
     * 면접 에이전트용 System Prompt 생성
     */
    private String buildInterviewAgentSystemPrompt(InterviewPhase phase) {
        String phaseDescription = phase == InterviewPhase.TECHNICAL
                ? "기술 면접"
                : "인성/태도 면접";

        String phaseGuidance = phase == InterviewPhase.TECHNICAL
                ? """
                    - 기술적 정확성, 깊이 있는 이해, 실무 적용 가능성을 중점적으로 평가합니다.
                    - 개념 설명, 비교 분석, 경험 기반 질문, 문제 해결, 아키텍처 설계 등의 질문 유형을 활용합니다.
                    - 각 기술 주제에 대해 최대 2개의 꼬리 질문(Deep Dive)을 허용합니다.
                    """
                : """
                    - 협업 능력, 갈등 해결, 리더십, 시간 관리, 스트레스 대처, 실패 경험, 성장 마인드셋 등을 평가합니다.
                    - STAR 기법(Situation, Task, Action, Result)을 활용한 구체적인 사례를 요구합니다.
                    - 각 인성 주제에 대해 최대 2개의 꼬리 질문(Deep Dive)을 허용합니다.
                    """;

        return """
            당신은 전문 면접관입니다. %s을 진행하고 있습니다.

            **면접관 페르소나:**
            - 친절하지만 전문적인 태도로 지원자를 평가합니다.
            - 지원자의 답변을 깊이 있게 분석하고, 적절한 시점에 심화 질문을 합니다.
            - 면접의 흐름과 시간을 관리하며, 충분히 평가가 완료되면 다음 단계로 넘어갑니다.

            **면접 진행 원칙:**
            %s
            - 꼬리 질문은 한 주제당 최대 2회까지만 허용합니다 (현재 주제의 꼬리 질문 횟수가 2회 이상이면 반드시 NEXT_PHASE를 반환).
            - 지원자의 답변이 충분히 깊이 있고 완성도가 높으면, 꼬리 질문 없이도 다음 주제나 다음 단계로 넘어갈 수 있습니다.
            - 면접 단계가 충분히 진행되었다고 판단되면 (일반적으로 3-5개의 주제를 다룬 후) NEXT_PHASE 액션을 반환합니다.

            **응답 형식:**
            - JSON 형식으로만 응답하세요 (마크다운 코드 블록 없이).
            - thought: 현재 상황에 대한 당신의 생각과 판단 근거
            - action: "GENERATE_QUESTION" 또는 "NEXT_PHASE"
            - message: action이 GENERATE_QUESTION일 때는 질문 내용, NEXT_PHASE일 때는 null
            - evaluation: 방금 받은 답변에 대한 평가 (선택적)
            """.formatted(phaseDescription, phaseGuidance);
    }

    /**
     * 면접 에이전트용 User Prompt 생성
     */
    private String buildInterviewAgentUserPrompt(
            InterviewPhase phase,
            String previousAnswer,
            String interviewContext,
            String resumeText,
            String portfolioText,
            List<String> previousQuestions,
            List<String> previousAnswers,
            int currentTopicFollowUpCount) {

        StringBuilder prompt = new StringBuilder();

        // 브릿지 답변 처리 (첫 질문 생성 시)
        if (previousAnswer != null && !previousAnswer.isBlank()) {
            prompt.append("**브릿지 답변 (Opening 단계의 마지막 답변):**\n");
            prompt.append(previousAnswer).append("\n\n");
            prompt.append("위 답변에서 언급된 기술/경험/키워드를 추출하여 첫 번째 질문을 생성하세요.\n");
            prompt.append("답변이 부실하거나 구체적이지 않다면, 이력서나 기술 스택 정보를 기반으로 질문을 생성하세요.\n\n");
        } else {
            prompt.append("**첫 질문 생성:**\n");
            prompt.append("이력서나 기술 스택 정보를 기반으로 첫 번째 질문을 생성하세요.\n\n");
        }

        // 면접 컨텍스트
        prompt.append("**면접 컨텍스트:**\n");
        prompt.append(interviewContext).append("\n\n");

        // 이력서/포트폴리오
        if (resumeText != null && !resumeText.isBlank()) {
            prompt.append("**이력서 내용:**\n");
            prompt.append(resumeText.substring(0, Math.min(resumeText.length(), 2000))).append("\n\n");
        }

        if (portfolioText != null && !portfolioText.isBlank()) {
            prompt.append("**포트폴리오 내용:**\n");
            prompt.append(portfolioText.substring(0, Math.min(portfolioText.length(), 2000)))
                    .append("\n\n");
        }

        // 이전 질문-답변 히스토리
        if (previousQuestions != null && !previousQuestions.isEmpty() &&
                previousAnswers != null && !previousAnswers.isEmpty()) {
            prompt.append("**이전 질문-답변 히스토리:**\n");
            int minSize = Math.min(previousQuestions.size(), previousAnswers.size());
            for (int i = 0; i < minSize; i++) {
                prompt.append(String.format("[질문 %d]\n", i + 1));
                prompt.append(previousQuestions.get(i)).append("\n");
                prompt.append("[답변]\n");
                prompt.append(previousAnswers.get(i)).append("\n\n");
            }
        }

        // 현재 주제의 꼬리 질문 횟수
        prompt.append("**현재 주제의 꼬리 질문 횟수:** ").append(currentTopicFollowUpCount).append("/2\n\n");

        // 지시사항
        if (previousQuestions == null || previousQuestions.isEmpty()) {
            prompt.append("**지시사항:**\n");
            prompt.append("첫 번째 질문을 생성하세요. action은 GENERATE_QUESTION이고, message에 질문 내용을 작성하세요.\n");
        } else {
            prompt.append("**지시사항:**\n");
            prompt.append("1. 마지막 답변을 평가하세요 (evaluation 필드에 간단히 작성).\n");
            prompt.append("2. 다음 행동을 결정하세요:\n");
            prompt.append("   - [꼬리 질문]: 현재 주제를 더 파고들어야 하면 -> action: GENERATE_QUESTION\n");
            prompt.append("   - [새로운 주제]: 현재 주제는 충분하지만, 아직 기술 면접을 더 진행해야 하면(다른 기술 질문) -> action: GENERATE_QUESTION (새로운 주제로 질문 생성)\n");
            prompt.append("   - [단계 종료]: 이미 3~4개의 대주제를 충분히 다루어, 이 기술 면접(Phase) 자체를 끝내도 된다면 -> action: NEXT_PHASE\n");
            prompt.append("3. thought 필드에 판단 근거를 작성하세요.\n");
        }

        prompt.append("\n**응답 형식 (JSON만, 마크다운 없이):**\n");
        prompt.append("""
            {
                "thought": "당신의 생각과 판단 근거",
                "action": "GENERATE_QUESTION" 또는 "NEXT_PHASE",
                "message": "질문 내용 (action이 GENERATE_QUESTION일 때만)",
                "evaluation": "답변 평가 (선택적)"
            }
            """);

        return prompt.toString();
    }
}
