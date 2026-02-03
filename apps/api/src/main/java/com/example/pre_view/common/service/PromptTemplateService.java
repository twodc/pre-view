package com.example.pre_view.common.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.pre_view.domain.interview.enums.InterviewPhase;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 프롬프트 템플릿을 관리하는 서비스
 *
 * resources/prompts/ 폴더의 텍스트 파일을 로드하고,
 * 플레이스홀더를 실제 값으로 치환하여 프롬프트를 생성합니다.
 */
@Slf4j
@Service
public class PromptTemplateService {

    private static final String PROMPTS_PATH = "prompts/";

    // 캐싱을 위한 Map
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("PromptTemplateService 초기화 - 프롬프트 템플릿 로드 시작");
        // 애플리케이션 시작 시 모든 프롬프트를 미리 로드
        preloadPrompts();
    }

    /**
     * 모든 프롬프트 파일을 미리 로드합니다.
     */
    private void preloadPrompts() {
        List<String> promptFiles = List.of(
                "feedback-system.txt",
                "feedback-user-opening.txt",
                "feedback-user-technical.txt",
                "feedback-user-personality.txt",
                "feedback-user-closing.txt",
                "report-user.txt",
                "interview-agent-system-technical.txt",
                "interview-agent-system-personality.txt",
                "interview-agent-user-first-question.txt",
                "interview-agent-user-continue.txt"
        );

        for (String fileName : promptFiles) {
            try {
                loadPrompt(fileName);
                log.debug("프롬프트 로드 완료: {}", fileName);
            } catch (Exception e) {
                log.warn("프롬프트 로드 실패 (나중에 다시 시도): {}", fileName);
            }
        }

        log.info("PromptTemplateService 초기화 완료 - {} 개의 프롬프트 로드됨", promptCache.size());
    }

    /**
     * 프롬프트 파일을 로드합니다.
     *
     * @param fileName 파일명 (예: "feedback-system.txt")
     * @return 프롬프트 내용
     */
    public String loadPrompt(String fileName) {
        return promptCache.computeIfAbsent(fileName, this::loadPromptFromFile);
    }

    /**
     * 파일에서 프롬프트를 읽어옵니다.
     */
    private String loadPromptFromFile(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPTS_PATH + fileName);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 파일 로드 실패: {}", fileName, e);
            throw new RuntimeException("프롬프트 파일을 로드할 수 없습니다: " + fileName, e);
        }
    }

    /**
     * 프롬프트에서 플레이스홀더를 치환합니다.
     *
     * @param template 프롬프트 템플릿
     * @param params   치환할 파라미터 맵
     * @return 치환된 프롬프트
     */
    public String fillTemplate(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 피드백 System Prompt를 반환합니다.
     */
    public String getFeedbackSystemPrompt() {
        return loadPrompt("feedback-system.txt");
    }

    /**
     * 단계별 피드백 User Prompt를 생성합니다.
     *
     * @param phase    면접 단계
     * @param question 질문 내용
     * @param answer   답변 내용
     * @return 완성된 피드백 프롬프트
     */
    public String buildFeedbackUserPrompt(InterviewPhase phase, String question, String answer) {
        String fileName = switch (phase) {
            case OPENING -> "feedback-user-opening.txt";
            case TECHNICAL -> "feedback-user-technical.txt";
            case PERSONALITY -> "feedback-user-personality.txt";
            case CLOSING -> "feedback-user-closing.txt";
        };

        String template = loadPrompt(fileName);
        Map<String, String> params = Map.of(
                "question", question,
                "answer", answer
        );
        return fillTemplate(template, params);
    }

    /**
     * 리포트 User Prompt를 생성합니다.
     *
     * @param position   포지션 정보
     * @param qnaContent 질문-답변 내용
     * @return 완성된 리포트 프롬프트
     */
    public String buildReportUserPrompt(String position, String qnaContent) {
        String template = loadPrompt("report-user.txt");
        Map<String, String> params = Map.of(
                "position", position,
                "qnaContent", qnaContent
        );
        return fillTemplate(template, params);
    }

    /**
     * 면접 에이전트 System Prompt를 반환합니다.
     *
     * @param phase 면접 단계 (TECHNICAL 또는 PERSONALITY)
     */
    public String getInterviewAgentSystemPrompt(InterviewPhase phase) {
        String fileName = phase == InterviewPhase.TECHNICAL
                ? "interview-agent-system-technical.txt"
                : "interview-agent-system-personality.txt";
        return loadPrompt(fileName);
    }

    /**
     * 면접 에이전트 User Prompt를 생성합니다 (첫 질문 생성용).
     */
    public String buildInterviewAgentFirstQuestionPrompt(
            String bridgeAnswer,
            String interviewContext,
            String resumeText,
            String portfolioText,
            int followUpCount) {

        String template = loadPrompt("interview-agent-user-first-question.txt");

        // 브릿지 섹션 생성
        String bridgeSection = "";
        if (bridgeAnswer != null && !bridgeAnswer.isBlank()) {
            bridgeSection = """
                    **브릿지 답변 (Opening 단계의 마지막 답변):**
                    %s

                    위 답변에서 언급된 기술/경험/키워드를 추출하여 첫 번째 질문을 생성하세요.
                    답변이 부실하거나 구체적이지 않다면, 면접 컨텍스트를 기반으로 질문을 생성하세요.
                    """.formatted(bridgeAnswer);
        }

        // 이력서 섹션
        String resumeSection = "";
        if (resumeText != null && !resumeText.isBlank()) {
            String truncated = resumeText.substring(0, Math.min(resumeText.length(), 2000));
            resumeSection = "**이력서 내용:**\n" + truncated;
        }

        // 포트폴리오 섹션
        String portfolioSection = "";
        if (portfolioText != null && !portfolioText.isBlank()) {
            String truncated = portfolioText.substring(0, Math.min(portfolioText.length(), 2000));
            portfolioSection = "**포트폴리오 내용:**\n" + truncated;
        }

        // 이력서/포트폴리오 없는 경우 안내
        String noDocumentNotice = "";
        if ((resumeText == null || resumeText.isBlank()) &&
                (portfolioText == null || portfolioText.isBlank())) {
            noDocumentNotice = """
                    **참고:** 이력서/포트폴리오가 제공되지 않았습니다.
                    면접 컨텍스트(포지션, 레벨, 기술스택)를 기반으로 질문을 생성하세요.
                    """;
        }

        Map<String, String> params = Map.of(
                "bridgeSection", bridgeSection,
                "interviewContext", interviewContext,
                "resumeSection", resumeSection,
                "portfolioSection", portfolioSection,
                "noDocumentNotice", noDocumentNotice,
                "followUpCount", String.valueOf(followUpCount)
        );

        return fillTemplate(template, params);
    }

    /**
     * 면접 에이전트 User Prompt를 생성합니다 (면접 진행 중).
     */
    public String buildInterviewAgentContinuePrompt(
            String interviewContext,
            String resumeText,
            String portfolioText,
            List<String> previousQuestions,
            List<String> previousAnswers,
            int followUpCount) {

        String template = loadPrompt("interview-agent-user-continue.txt");

        // 이력서 섹션
        String resumeSection = "";
        if (resumeText != null && !resumeText.isBlank()) {
            String truncated = resumeText.substring(0, Math.min(resumeText.length(), 2000));
            resumeSection = "**이력서 내용:**\n" + truncated;
        }

        // 포트폴리오 섹션
        String portfolioSection = "";
        if (portfolioText != null && !portfolioText.isBlank()) {
            String truncated = portfolioText.substring(0, Math.min(portfolioText.length(), 2000));
            portfolioSection = "**포트폴리오 내용:**\n" + truncated;
        }

        // Q&A 히스토리 생성
        StringBuilder qaHistory = new StringBuilder();
        int minSize = Math.min(previousQuestions.size(), previousAnswers.size());
        for (int i = 0; i < minSize; i++) {
            qaHistory.append(String.format("[질문 %d]\n", i + 1));
            qaHistory.append(previousQuestions.get(i)).append("\n");
            qaHistory.append("[답변]\n");
            qaHistory.append(previousAnswers.get(i)).append("\n\n");
        }

        Map<String, String> params = Map.of(
                "interviewContext", interviewContext,
                "resumeSection", resumeSection,
                "portfolioSection", portfolioSection,
                "qaHistory", qaHistory.toString(),
                "followUpCount", String.valueOf(followUpCount)
        );

        return fillTemplate(template, params);
    }

    /**
     * 캐시를 새로고침합니다 (프롬프트 파일 변경 시 사용).
     */
    public void refreshCache() {
        log.info("프롬프트 캐시 새로고침 시작");
        promptCache.clear();
        preloadPrompts();
        log.info("프롬프트 캐시 새로고침 완료");
    }
}
