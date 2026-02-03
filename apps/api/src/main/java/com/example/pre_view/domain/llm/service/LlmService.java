package com.example.pre_view.domain.llm.service;

import java.time.Duration;
import java.util.List;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.llm.config.LlmConfig;
import com.example.pre_view.domain.llm.dto.LlmFeedbackRequest;
import com.example.pre_view.domain.llm.dto.LlmFeedbackResponse;
import com.example.pre_view.domain.llm.dto.LlmInterviewRequest;
import com.example.pre_view.domain.llm.dto.LlmInterviewResponse;
import com.example.pre_view.domain.llm.dto.LlmReportRequest;
import com.example.pre_view.domain.llm.dto.LlmReportResponse;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LlmService {

    private final LlmConfig llmConfig;
    private final MeterRegistry meterRegistry;
    private RestClient restClient;

    private Timer llmFeedbackTimer;
    private Timer llmInterviewTimer;
    private Timer llmReportTimer;
    private Counter llmCallSuccessCounter;
    private Counter llmCallFailureCounter;

    public LlmService(LlmConfig llmConfig, MeterRegistry meterRegistry) {
        this.llmConfig = llmConfig;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(llmConfig.getTimeout()));
        factory.setReadTimeout(Duration.ofMillis(llmConfig.getTimeout()));

        this.restClient = RestClient.builder()
                .baseUrl(llmConfig.getServiceUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.llmFeedbackTimer = Timer.builder("llm.feedback.duration")
                .description("LLM 피드백 생성 응답 시간")
                .register(meterRegistry);
        this.llmInterviewTimer = Timer.builder("llm.interview.duration")
                .description("LLM 면접 에이전트 응답 시간")
                .register(meterRegistry);
        this.llmReportTimer = Timer.builder("llm.report.duration")
                .description("LLM 리포트 생성 응답 시간")
                .register(meterRegistry);
        this.llmCallSuccessCounter = Counter.builder("llm.calls.success")
                .description("LLM 호출 성공 횟수")
                .register(meterRegistry);
        this.llmCallFailureCounter = Counter.builder("llm.calls.failure")
                .description("LLM 호출 실패 횟수")
                .register(meterRegistry);

        log.info("LlmService 초기화 완료 - serviceUrl: {}, enabled: {}",
                llmConfig.getServiceUrl(), llmConfig.isEnabled());
    }

    @Retry(name = "llmServiceRetry", fallbackMethod = "generateFeedbackFallback")
    public LlmFeedbackResponse generateFeedback(String systemPrompt, String userPrompt) {
        log.debug("LLM 피드백 생성 시작");

        return llmFeedbackTimer.record(() -> {
            try {
                LlmFeedbackRequest request = new LlmFeedbackRequest(systemPrompt, userPrompt);

                LlmFeedbackResponse response = restClient.post()
                        .uri("/api/feedback")
                        .body(request)
                        .retrieve()
                        .body(LlmFeedbackResponse.class);

                llmCallSuccessCounter.increment();
                log.debug("LLM 피드백 생성 완료 - score: {}", response != null ? response.score() : null);
                return response;
            } catch (Exception e) {
                llmCallFailureCounter.increment();
                log.error("LLM 피드백 생성 실패", e);
                throw new BusinessException(ErrorCode.AI_RESPONSE_ERROR);
            }
        });
    }

    public LlmFeedbackResponse generateFeedbackFallback(String systemPrompt, String userPrompt, Exception e) {
        log.error("LLM 피드백 생성 실패 (Fallback) - 기본 응답 반환", e);
        llmCallFailureCounter.increment();
        return new LlmFeedbackResponse(
                "LLM 서비스 연결 문제로 자동 피드백을 생성할 수 없었습니다.",
                5, false, null);
    }

    @Retry(name = "llmServiceRetry", fallbackMethod = "processInterviewStepFallback")
    public LlmInterviewResponse processInterviewStep(String systemPrompt, String userPrompt) {
        log.debug("LLM 면접 에이전트 단계 처리 시작");

        return llmInterviewTimer.record(() -> {
            try {
                LlmInterviewRequest request = new LlmInterviewRequest(systemPrompt, userPrompt);

                LlmInterviewResponse response = restClient.post()
                        .uri("/api/interview/step")
                        .body(request)
                        .retrieve()
                        .body(LlmInterviewResponse.class);

                llmCallSuccessCounter.increment();
                log.debug("LLM 면접 에이전트 완료 - action: {}", response != null ? response.action() : null);
                return response;
            } catch (Exception e) {
                llmCallFailureCounter.increment();
                log.error("LLM 면접 에이전트 실패", e);
                throw new BusinessException(ErrorCode.AI_RESPONSE_ERROR);
            }
        });
    }

    public LlmInterviewResponse processInterviewStepFallback(String systemPrompt, String userPrompt, Exception e) {
        log.error("LLM 면접 에이전트 실패 (Fallback) - null 반환", e);
        llmCallFailureCounter.increment();
        return null;
    }

    @Retry(name = "llmServiceRetry", fallbackMethod = "generateReportFallback")
    public LlmReportResponse generateReport(String systemPrompt, String userPrompt) {
        log.debug("LLM 리포트 생성 시작");

        return llmReportTimer.record(() -> {
            try {
                LlmReportRequest request = new LlmReportRequest(systemPrompt, userPrompt);

                LlmReportResponse response = restClient.post()
                        .uri("/api/report")
                        .body(request)
                        .retrieve()
                        .body(LlmReportResponse.class);

                llmCallSuccessCounter.increment();
                log.debug("LLM 리포트 생성 완료 - overallScore: {}", response != null ? response.overallScore() : null);
                return response;
            } catch (Exception e) {
                llmCallFailureCounter.increment();
                log.error("LLM 리포트 생성 실패", e);
                throw new BusinessException(ErrorCode.AI_RESPONSE_ERROR);
            }
        });
    }

    public LlmReportResponse generateReportFallback(String systemPrompt, String userPrompt, Exception e) {
        log.error("LLM 리포트 생성 실패 (Fallback) - 기본 응답 반환", e);
        llmCallFailureCounter.increment();
        return new LlmReportResponse(
                "LLM 서비스 연결 문제로 리포트를 생성할 수 없습니다.",
                List.of(), List.of(), List.of(), 0, List.of());
    }

    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("LLM 서비스 헬스체크 실패", e);
            return false;
        }
    }
}
