package com.example.pre_view.loadtest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.pre_view.loadtest.LoadTestController.AiSimulationRequest;
import com.example.pre_view.loadtest.LoadTestController.AiSimulationResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 부하 테스트용 서비스
 *
 * WireMock 서버에 HTTP 요청을 보내 AI API 호출을 시뮬레이션합니다.
 * 실제 I/O 블로킹이 발생하여 가상 스레드의 효과를 측정할 수 있습니다.
 */
@Slf4j
@Service
@Profile("loadtest")
public class LoadTestService {

    private final RestClient restClient;

    public LoadTestService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:9090")
                .build();
    }

    /**
     * AI API 호출 시뮬레이션
     *
     * WireMock 서버로 요청을 보내 2초 딜레이를 시뮬레이션합니다.
     * 이 딜레이 동안 가상 스레드는 다른 요청을 처리할 수 있습니다.
     */
    public AiSimulationResponse simulateAiCall(AiSimulationRequest request) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().toString();

        log.debug("AI 시뮬레이션 시작 - thread: {}", threadName);

        try {
            // WireMock 서버로 요청 (2초 딜레이 발생)
            restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .body("""
                            {
                                "model": "test",
                                "messages": [{"role": "user", "content": "%s"}]
                            }
                            """.formatted(request.content()))
                    .retrieve()
                    .body(String.class);

            long duration = System.currentTimeMillis() - startTime;

            return new AiSimulationResponse(
                    "Mock AI 피드백: 좋은 답변입니다.",
                    8,
                    duration,
                    threadName
            );

        } catch (Exception e) {
            log.warn("WireMock 호출 실패, fallback 응답 반환: {}", e.getMessage());

            // WireMock이 없을 경우 Thread.sleep으로 대체
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            long duration = System.currentTimeMillis() - startTime;

            return new AiSimulationResponse(
                    "Fallback 피드백: AI 서버 연결 실패",
                    5,
                    duration,
                    threadName
            );
        }
    }
}
