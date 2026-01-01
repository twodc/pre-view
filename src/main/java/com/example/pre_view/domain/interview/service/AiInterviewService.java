package com.example.pre_view.domain.interview.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.pre_view.domain.answer.dto.AiFeedbackResponse;
import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.dto.AiReportResponse;
import com.example.pre_view.domain.interview.dto.AiSingleQuestionResponse;
import com.example.pre_view.domain.interview.enums.InterviewPhase;

import io.github.resilience4j.retry.annotation.Retry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiInterviewService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a professional interviewer conducting job interviews.

            CRITICAL OUTPUT LANGUAGE RULE:
            - ALL text outputs (feedback, questions, responses) MUST be written EXCLUSIVELY in Korean (한국어).
            - NEVER use English, Chinese, Japanese, or any other languages in output text.
            - Even though these instructions are in English, your output must be 100% Korean.

            Response format:
            - Provide responses in JSON format only, without markdown formatting.
            - Be specific and constructive in your feedback.
            - Provide detailed feedback (5-7 sentences) covering strengths, weaknesses, and improvement suggestions.
            """;

    public AiInterviewService(ChatClient.Builder builder) {
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
                        Follow-up Question Generation Rules (CRITICAL):
                        - Only generate a follow-up question if the answer is reasonably good (score >= 5) but needs slight clarification or additional details.
                        - DO NOT generate follow-up questions if:
                          * The score is 4 or below (the candidate clearly struggled with the question - just give low score and move on)
                          * The answer is completely wrong or shows fundamental misunderstanding
                          * The answer is too vague or too short to evaluate properly
                        - Follow-up questions should ask for ADDITIONAL details or examples, NOT repeat the same question in different words.
                        - Each main question should have AT MOST ONE follow-up question.
                        - The follow-up question must be DIFFERENT from the original question (avoid similar phrasing).

                        If the above conditions are met, set needsFollowUp to true and provide a follow-up question in Korean ONLY.
                        Otherwise, set needsFollowUp to false and followUpQuestion to null.

                        Examples of good follow-up questions (MUST be in Korean):
                        - "해당 경험에서 가장 어려웠던 부분은 무엇이었나요?"
                        - "이 기술을 실제 프로젝트에 어떻게 적용하셨는지 구체적인 예시를 들어주실 수 있나요?"
                        - "이런 상황에서 발생할 수 있는 문제점이나 트레이드오프는 무엇이라고 생각하시나요?"
                        """
                : "Set needsFollowUp to false and followUpQuestion to null.";

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
                                Evaluate the candidate's answer based on the interview phase.

                                Interview Phase: {phase}
                                Question: {question}
                                Answer: {answer}

                                Evaluation Criteria: {criteria}

                                {phaseInstruction}

                                {feedbackGuideline}

                                {followUpInstruction}

                                OUTPUT LANGUAGE REQUIREMENT (CRITICAL):
                                - ALL text in your response MUST be written EXCLUSIVELY in Korean (한국어).
                                - Do NOT use English, Chinese, Japanese, or any other languages.
                                - The feedback field and followUpQuestion field (if any) must be 100% Korean.

                                Feedback Structure Requirements:
                                - Write 5-7 detailed sentences in Korean
                                - Cover each evaluation criterion specifically
                                - Include: (1) Strengths (what was good), (2) Areas for improvement (what was lacking), (3) Specific improvement suggestions

                                Example Feedback Format (Korean only):
                                {feedbackExample}

                                Response Format (JSON only, no markdown):
                                {{
                                    "feedback": "한국어로 작성된 구체적이고 상세한 피드백 (5-7문장)",
                                    "score": 7,
                                    "needsFollowUp": false,
                                    "followUpQuestion": null
                                }}

                                Score should be between 1 and 10.
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
            case GREETING -> "자연스러움, 예의, 긍정적인 태도";
            case SELF_INTRO -> "명확성, 관련 경험 언급, 지원 동기의 진정성, 논리적 구성";
            case PERSONALITY -> "구체적인 사례(STAR 기법), 자기 인식, 협업 능력, 문제 해결 접근법, 성장 의지";
            case TECHNICAL -> "기술적 정확성, 깊이 있는 이해, 실무 적용 가능성, 문제 해결 능력";
            case CLOSING -> "적극성, 회사/직무에 대한 관심, 준비성";
        };
    }

    /**
     * 단계별 구체적인 지시사항 (영어로 작성하여 명확성 확보)
     */
    private String getPhaseSpecificInstruction(InterviewPhase phase) {
        return switch (phase) {
            case GREETING -> """
                    IMPORTANT: This is the GREETING phase.
                    Focus ONLY on the candidate's greeting, manners, attitude, and first impression.
                    Do NOT ask technical questions or dig into specific experiences.
                    """;
            case SELF_INTRO -> """
                    IMPORTANT: This is the SELF-INTRODUCTION phase.
                    Focus ONLY on the candidate's background, motivation, and career goals.
                    Do NOT ask technical questions or dig into technical details.
                    Technical questions will be asked in the TECHNICAL phase.
                    """;
            case PERSONALITY ->
                """
                        IMPORTANT: This is the PERSONALITY/BEHAVIORAL phase.
                        Focus on the candidate's collaboration skills, conflict resolution, leadership, and other personality aspects.
                        Evaluate whether they use the STAR method (Situation, Task, Action, Result) with specific examples.
                        """;
            case TECHNICAL ->
                """
                        IMPORTANT: This is the TECHNICAL phase.
                        Evaluate the candidate's technical knowledge, understanding, and practical application abilities.
                        Focus on technical accuracy and deep understanding.
                        """;
            case CLOSING ->
                """
                        IMPORTANT: This is the CLOSING phase.
                        Focus on the candidate's proactiveness, interest in the company/position, and preparedness.
                        """;
        };
    }

    /**
     * 단계별 피드백 가이드라인 (영어로 작성하여 명확성 확보)
     */
    private String getFeedbackGuideline(InterviewPhase phase) {
        return switch (phase) {
            case GREETING -> """
                    Feedback Writing Guidelines (write feedback in Korean):
                    - Evaluate whether the greeting was natural and polite
                    - Mention whether there was a positive attitude and confident start
                    - Provide specific evaluation of first impression
                    - Suggest how to start more confidently if needed
                    """;
            case SELF_INTRO -> """
                    Feedback Writing Guidelines (write feedback in Korean):
                    - Evaluate whether the self-introduction was clear and logically structured
                    - Check if relevant experiences were mentioned specifically
                    - Evaluate whether the motivation was expressed sincerely
                    - Present areas for improvement and specific improvement suggestions
                    """;
            case PERSONALITY -> """
                    Feedback Writing Guidelines (write feedback in Korean):
                    - Evaluate whether specific examples using STAR method were provided
                    - Assess self-awareness and growth mindset
                    - Evaluate collaboration skills and problem-solving approach
                    - Provide specific advice on areas needing improvement
                    """;
            case TECHNICAL -> """
                    Feedback Writing Guidelines (write feedback in Korean):
                    - Evaluate technical accuracy and depth of understanding
                    - Assess practical application potential
                    - Evaluate problem-solving ability and approach
                    - Suggest specific learning directions for lacking areas
                    """;
            case CLOSING -> """
                    Feedback Writing Guidelines (write feedback in Korean):
                    - Evaluate proactiveness and level of interest
                    - Assess understanding and preparedness regarding company/position
                    - Evaluate whether the closing was impressive
                    - Suggest additional points to prepare
                    """;
        };
    }

    /**
     * 단계별 피드백 예시 (Few-shot learning을 위한 한국어 예시)
     */
    private String getFeedbackExample(InterviewPhase phase) {
        return switch (phase) {
            case GREETING ->
                """
                        "인사말이 자연스럽고 예의 바른 태도가 좋았습니다. 긍정적인 첫인상을 주었으며, 대화의 시작이 매끄럽게 이루어졌습니다. 다만, 조금 더 자신감 있는 어조로 시작한다면 면접관에게 더 강한 인상을 줄 수 있을 것입니다. 목소리의 크기와 속도도 적절했습니다. 앞으로는 면접 초반에 자신의 강점을 간단히 언급하는 것도 좋은 방법입니다."
                        """;
            case SELF_INTRO ->
                """
                        "자기소개가 명확하고 논리적으로 구성되어 있어 이해하기 쉬웠습니다. 특히 관련 경험을 구체적으로 언급하여 지원 동기의 진정성을 잘 보여주었습니다. 다만, 지원 동기를 설명할 때 더 구체적인 사례나 데이터를 활용한다면 설득력이 더욱 높아질 것입니다. 또한 자신의 강점을 단순 나열이 아닌 스토리텔링 방식으로 전달하면 더욱 효과적일 것입니다."
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
     * 이력서/포트폴리오/기술스택 기반으로 질문을 실시간 생성
     * 
     * @param phase            면접 단계 (PERSONALITY 또는 TECHNICAL)
     * @param interviewContext 면접 컨텍스트 (포지션, 레벨, 기술스택 등)
     * @param resumeText       이력서 텍스트 (PDF에서 추출된 텍스트, 선택)
     * @param portfolioText    포트폴리오 텍스트 (PDF에서 추출된 텍스트, 선택)
     * @param previousAnswers  이전 답변 목록 (이전 답변 기반 추가 질문 생성 시 사용)
     * @return 생성된 질문 텍스트
     */
    @Retry(name = "aiServiceRetry", fallbackMethod = "recoverQuestion")
    public String generateQuestionByContext(
            InterviewPhase phase,
            String interviewContext,
            String resumeText,
            String portfolioText,
            List<String> previousAnswers) {

        String previousAnswersText = previousAnswers != null && !previousAnswers.isEmpty()
                ? String.join("\n", previousAnswers)
                : null;

        String resumeSection = resumeText != null && !resumeText.isBlank()
                ? "\n\nResume Content:\n" + resumeText
                : "";
        String portfolioSection = portfolioText != null && !portfolioText.isBlank()
                ? "\n\nPortfolio Content:\n" + portfolioText
                : "";
        String previousAnswersSection = previousAnswersText != null && !previousAnswersText.isBlank()
                ? "\n\nPrevious Answers:\n" + previousAnswersText +
                        "\n\nBased on the previous answers, generate a deeper follow-up question."
                : "";

        String promptTemplate = buildQuestionPromptTemplate(phase);

        // 파라미터를 Map으로 그룹화하여 .params() 사용
        Map<String, Object> params = Map.of(
                "interviewContext", interviewContext,
                "resumeSection", resumeSection,
                "portfolioSection", portfolioSection,
                "previousAnswersSection", previousAnswersSection);

        AiSingleQuestionResponse response = chatClient.prompt()
                .user(u -> u
                        .text(promptTemplate)
                        .params(params))
                .call()
                .entity(AiSingleQuestionResponse.class);
        return response.question();
    }

    /**
     * generateQuestionByContext의 Fallback 메서드
     * 모든 재시도 실패 시 호출됨
     */
    public String recoverQuestion(InterviewPhase phase, String interviewContext,
                                  String resumeText, String portfolioText, List<String> previousAnswers, Exception e) {
        log.error("AI 질문 생성 실패 (모든 재시도 실패) - phase: {}, Fallback 질문 반환", phase, e);
        return getFallbackQuestion(phase);
    }

    /**
     * 질문 생성용 프롬프트 템플릿 생성
     */
    private String buildQuestionPromptTemplate(InterviewPhase phase) {
        return switch (phase) {
            case PERSONALITY ->
                """
                        Generate a personality/behavioral interview question based on the candidate's information.

                        Interview Context: {interviewContext}
                        {resumeSection}
                        {portfolioSection}
                        {previousAnswersSection}

                        Focus on: teamwork, conflict resolution, leadership, time management,
                        stress handling, failure experiences, growth mindset.
                        Use the STAR method (Situation, Task, Action, Result) when appropriate.

                        CRITICAL: The question must be written in Korean language (한국어) only.
                        Do NOT use English, Chinese, Japanese, or any other languages.

                        Response format (JSON only, no markdown):
                        {{"question": "한국어로 작성된 질문"}}
                        """;
            case TECHNICAL ->
                """
                        Generate a technical interview question tailored to the candidate.

                        Interview Context: {interviewContext}
                        {resumeSection}
                        {portfolioSection}
                        {previousAnswersSection}

                        Question types to consider:
                        - Concept explanation (e.g., "Explain the 4 principles of OOP")
                        - Comparison questions (e.g., "What's the difference between Array and ArrayList?")
                        - Experience-based questions (e.g., "How did you apply this in a real project?")
                        - Problem-solving questions (e.g., "How would you solve this situation?")
                        - Architecture design questions

                        CRITICAL: The question must be written in Korean language (한국어) only.
                        Do NOT use English, Chinese, Japanese, or any other languages.

                        Response format (JSON only, no markdown):
                        {{"question": "한국어로 작성된 질문"}}
                        """;
            default ->
                throw new IllegalArgumentException("AI 생성 질문은 PERSONALITY 또는 TECHNICAL 단계에서만 가능합니다.");
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
                                Create a comprehensive interview report analyzing the candidate's performance.

                                Position: {position}

                                Questions, Answers, and Scores by Interview Phase:

                                {qnaContent}

                                CRITICAL: All text fields (summary, strengths, improvements, recommendedTopics)
                                must be written in Korean language (한국어) only.
                                Do NOT use English, Chinese, Japanese, or any other languages.

                                Response format (JSON only, no markdown):
                                {{
                                    "summary": "종합 평가 (2-3문장)",
                                    "strengths": ["강점1", "강점2"],
                                    "improvements": ["개선점1", "개선점2"],
                                    "recommendedTopics": ["추천 학습 주제1", "추천 학습 주제2"],
                                    "overallScore": 7
                                }}

                                overallScore should be between 1 and 10, calculated as an average considering all answers.
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
}
