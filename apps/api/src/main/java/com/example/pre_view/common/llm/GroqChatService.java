package com.example.pre_view.common.llm;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import io.micrometer.core.instrument.Counter;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GroqChatService {

    private final GroqConfig config;
    private final JsonMapper objectMapper;
    private RestClient restClient;

    // 메트릭
    private Counter primaryModelCounter;
    private Counter fallbackModelCounter;
    private Counter rateLimitCounter;

    public GroqChatService(GroqConfig config, JsonMapper objectMapper, MeterRegistry meterRegistry) {
        this.config = config;
        this.objectMapper = objectMapper;

        this.primaryModelCounter = Counter.builder("groq.model.primary")
                .description("Primary 모델 사용 횟수")
                .register(meterRegistry);
        this.fallbackModelCounter = Counter.builder("groq.model.fallback")
                .description("Fallback 모델 사용 횟수")
                .register(meterRegistry);
        this.rateLimitCounter = Counter.builder("groq.ratelimit.hit")
                .description("Rate limit 발생 횟수")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(config.getTimeout()));
        factory.setReadTimeout(Duration.ofMillis(config.getTimeout()));

        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("GroqChatService 초기화 - primary: {}, fallback: {}",
                config.getPrimaryModel(), config.getFallbackModel());
    }

    /**
     * 채팅 완료 요청 (Failover 지원)
     * Primary 모델에서 429 에러 시 Fallback 모델로 자동 전환
     */
    public <T> T chatCompletion(String systemPrompt, String userPrompt, Class<T> responseType) {
        // 1차: Primary 모델 시도
        try {
            String response = callGroqApi(config.getPrimaryModel(), systemPrompt, userPrompt);
            primaryModelCounter.increment();
            log.debug("Primary 모델 응답 성공 - model: {}", config.getPrimaryModel());
            return parseJsonResponse(response, responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Primary 모델 rate limit 도달 - model: {}, fallback으로 전환", config.getPrimaryModel());
            rateLimitCounter.increment();
        } catch (Exception e) {
            log.warn("Primary 모델 호출 실패 - model: {}, error: {}", config.getPrimaryModel(), e.getMessage());
        }

        // 2차: Fallback 모델 시도
        try {
            String response = callGroqApi(config.getFallbackModel(), systemPrompt, userPrompt);
            fallbackModelCounter.increment();
            log.info("Fallback 모델 응답 성공 - model: {}", config.getFallbackModel());
            return parseJsonResponse(response, responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("Fallback 모델도 rate limit 도달 - model: {}", config.getFallbackModel());
            rateLimitCounter.increment();
            throw new RuntimeException("모든 LLM 모델이 rate limit에 도달했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (Exception e) {
            log.error("Fallback 모델 호출 실패 - model: {}", config.getFallbackModel(), e);
            throw new RuntimeException("LLM 서비스 호출 실패", e);
        }
    }

    /**
     * Groq API 직접 호출
     */
    private String callGroqApi(String model, String systemPrompt, String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens(),
                "response_format", Map.of("type", "json_object")
        );

        log.debug("Groq API 호출 - model: {}", model);

        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(Map.class);

        // 응답에서 content 추출
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    /**
     * JSON 응답을 객체로 파싱
     */
    private <T> T parseJsonResponse(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (Exception e) {
            log.error("JSON 파싱 실패 - response: {}", json, e);
            throw new RuntimeException("LLM 응답 파싱 실패", e);
        }
    }

    /**
     * 현재 사용 중인 모델 정보
     */
    public String getPrimaryModel() {
        return config.getPrimaryModel();
    }

    public String getFallbackModel() {
        return config.getFallbackModel();
    }
}
