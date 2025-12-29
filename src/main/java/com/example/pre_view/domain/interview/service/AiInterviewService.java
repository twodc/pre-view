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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiInterviewService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a professional interviewer conducting job interviews.
            - Always respond in Korean language.
            - Provide responses in JSON format only, without markdown formatting.
            - Be specific and constructive in your feedback.
            """;

    public AiInterviewService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /**
     * 답변에 대한 AI 피드백 생성
     */
    public AiFeedbackResponse generateFeedback(InterviewPhase phase, String question, String answer) {
        String evaluationCriteria = getEvaluationCriteria(phase);
        boolean allowFollowUp = phase.isAllowFollowUp();

        String phaseSpecificInstruction = phase == InterviewPhase.SELF_INTRO
                ? """
                        IMPORTANT: This is a SELF-INTRODUCTION phase. 
                        Focus ONLY on the candidate's background, motivation, and career goals.
                        Do NOT ask technical questions or dig into technical details.
                        Technical questions will be asked in the TECHNICAL phase.
                        """
                : "";

        String followUpInstruction = allowFollowUp
                ? """
                        If the answer lacks depth or needs clarification within THIS phase's scope, 
                        set needsFollowUp to true and provide a follow-up question.
                        Otherwise, set needsFollowUp to false and followUpQuestion to null.
                        
                        For follow-up questions, dig deeper into incomplete answers within the same phase context.
                        Examples:
                        - "Could you explain that part in more detail?"
                        - "Do you have any specific experience or examples?"
                        - "Why do you think that way?"
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
        
        return chatClient.prompt()
                .user(u -> u
                        .text("""
                                Evaluate the candidate's answer based on the interview phase.
                                
                                Interview Phase: {phase}
                                Question: {question}
                                Answer: {answer}
                                
                                Evaluation criteria: {criteria}
                                
                                {phaseInstruction}
                                
                                {followUpInstruction}
                                
                                Response format (JSON only, no markdown):
                                {
                                    "feedback": "구체적인 피드백 (2-3문장)",
                                    "score": 7,
                                    "needsFollowUp": false,
                                    "followUpQuestion": null
                                }
                                
                                Score should be between 1 and 10.
                                """)
                        .params(params))
                .call()
                .entity(AiFeedbackResponse.class);
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
     * 이력서/포트폴리오/기술스택 기반으로 질문을 실시간 생성
     * @param phase 면접 단계 (PERSONALITY 또는 TECHNICAL)
     * @param interviewContext 면접 컨텍스트 (포지션, 레벨, 기술스택 등)
     * @param resumeText 이력서 텍스트 (PDF에서 추출된 텍스트, 선택)
     * @param portfolioText 포트폴리오 텍스트 (PDF에서 추출된 텍스트, 선택)
     * @param previousAnswers 이전 답변 목록 (이전 답변 기반 추가 질문 생성 시 사용)
     * @return 생성된 질문 텍스트
     */
    public String generateQuestionByContext(
            InterviewPhase phase, 
            String interviewContext, 
            String resumeText, 
            String portfolioText,
            List<String> previousAnswers) {
        
        String previousAnswersText = previousAnswers != null && !previousAnswers.isEmpty()
                ? String.join("\n", previousAnswers)
                : null;
        
        try {
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
                    "previousAnswersSection", previousAnswersSection
            );
            
            AiSingleQuestionResponse response = chatClient.prompt()
                    .user(u -> u
                            .text(promptTemplate)
                            .params(params))
                    .call()
                    .entity(AiSingleQuestionResponse.class);
            
            return response.question();
        } catch (Exception e) {
            log.error("AI 질문 생성 중 오류 발생 - phase: {}", phase, e);
            return getFallbackQuestion(phase);
        }
    }
    
    /**
     * 질문 생성용 프롬프트 템플릿 생성
     */
    private String buildQuestionPromptTemplate(InterviewPhase phase) {
        return switch (phase) {
            case PERSONALITY -> """
                    Generate a personality/behavioral interview question based on the candidate's information.
                    
                    Interview Context: {interviewContext}
                    {resumeSection}
                    {portfolioSection}
                    {previousAnswersSection}
                    
                    Focus on: teamwork, conflict resolution, leadership, time management, 
                    stress handling, failure experiences, growth mindset.
                    Use the STAR method (Situation, Task, Action, Result) when appropriate.
                    
                    Response format (JSON only, no markdown):
                    {"question": "생성된 질문"}
                    """;
            case TECHNICAL -> """
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
                    
                    Response format (JSON only, no markdown):
                    {"question": "생성된 질문"}
                    """;
            default -> throw new IllegalArgumentException("AI 생성 질문은 PERSONALITY 또는 TECHNICAL 단계에서만 가능합니다.");
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
                                
                                Response format (JSON only, no markdown):
                                {
                                    "summary": "종합 평가 (2-3문장)",
                                    "strengths": ["강점1", "강점2"],
                                    "improvements": ["개선점1", "개선점2"],
                                    "recommendedTopics": ["추천 학습 주제1", "추천 학습 주제2"],
                                    "overallScore": 7
                                }
                                
                                overallScore should be between 1 and 10, calculated as an average considering all answers.
                                """)
                        .param("position", context)
                        .param("qnaContent", qnaContent))
                .call()
                .entity(AiReportResponse.class);
    }
}
