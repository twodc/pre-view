package com.example.pre_view.loadtest;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 부하 테스트용 컨트롤러
 *
 * loadtest 프로파일에서만 활성화됩니다.
 * 인증 없이 AI 호출 시뮬레이션을 테스트할 수 있습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/loadtest")
@RequiredArgsConstructor
@Profile("loadtest")
public class LoadTestController {

    private final LoadTestService loadTestService;

    /**
     * 헬스 체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Load test endpoint is ready"));
    }

    /**
     * AI 호출 시뮬레이션 엔드포인트
     *
     * 실제 AI API 대신 WireMock을 호출하여 I/O 대기 시간을 시뮬레이션합니다.
     * 가상 스레드의 효과를 측정하기 위한 엔드포인트입니다.
     */
    @PostMapping("/ai-simulation")
    public ResponseEntity<ApiResponse<AiSimulationResponse>> simulateAiCall(
            @RequestBody AiSimulationRequest request
    ) {
        log.debug("AI 시뮬레이션 요청 - content length: {}", request.content().length());

        long startTime = System.currentTimeMillis();
        AiSimulationResponse response = loadTestService.simulateAiCall(request);
        long duration = System.currentTimeMillis() - startTime;

        log.debug("AI 시뮬레이션 완료 - duration: {}ms", duration);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record AiSimulationRequest(String content) {}

    public record AiSimulationResponse(
            String feedback,
            int score,
            long processingTimeMs,
            String threadName
    ) {}
}
