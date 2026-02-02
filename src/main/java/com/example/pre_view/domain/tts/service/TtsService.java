package com.example.pre_view.domain.tts.service;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.tts.config.TtsConfig;
import com.example.pre_view.domain.tts.dto.SynthesizeRequest;
import com.example.pre_view.domain.tts.dto.SynthesizeResponse;
import com.example.pre_view.domain.tts.dto.TtsHealthResponse;
import com.example.pre_view.domain.tts.dto.VoiceInfo;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * TTS 서비스
 *
 * Python TTS 서비스(tts-service:8002)와 통신하여 텍스트를 음성으로 변환합니다.
 */
@Slf4j
@Service
public class TtsService {

    private final TtsConfig ttsConfig;
    private final RestClient restClient;

    // 메트릭
    private final Timer synthesizeTimer;
    private final Counter ttsCallSuccessCounter;
    private final Counter ttsCallFailureCounter;

    public TtsService(TtsConfig ttsConfig, MeterRegistry meterRegistry) {
        this.ttsConfig = ttsConfig;
        this.restClient = RestClient.builder()
                .baseUrl(ttsConfig.getServiceUrl())
                .build();

        // 메트릭 등록
        this.synthesizeTimer = Timer.builder("tts.synthesize.duration")
                .description("TTS 음성 합성 응답 시간")
                .register(meterRegistry);
        this.ttsCallSuccessCounter = Counter.builder("tts.calls.success")
                .description("TTS API 호출 성공 횟수")
                .register(meterRegistry);
        this.ttsCallFailureCounter = Counter.builder("tts.calls.failure")
                .description("TTS API 호출 실패 횟수")
                .register(meterRegistry);
    }

    /**
     * 텍스트를 음성으로 변환 (Base64 응답)
     *
     * @param request 음성 합성 요청
     * @return Base64 인코딩된 오디오 데이터
     */
    @Retry(name = "ttsServiceRetry", fallbackMethod = "recoverSynthesize")
    public SynthesizeResponse synthesize(SynthesizeRequest request) {
        log.debug("TTS 음성 합성 시작 - text length: {}, voice: {}, speed: {}, format: {}",
                request.text().length(), request.voice(), request.speed(), request.format());

        // 텍스트 길이 검증
        if (request.text().length() > ttsConfig.getMaxTextLength()) {
            log.warn("TTS 텍스트 길이 초과 - length: {}, max: {}",
                    request.text().length(), ttsConfig.getMaxTextLength());
            throw new BusinessException(ErrorCode.TTS_TEXT_TOO_LONG);
        }

        return synthesizeTimer.record(() -> {
            try {
                SynthesizeResponse response = restClient.post()
                        .uri("/api/synthesize")
                        .body(request)
                        .retrieve()
                        .body(SynthesizeResponse.class);

                ttsCallSuccessCounter.increment();
                log.info("TTS 음성 합성 완료 - duration: {}s, format: {}, sampleRate: {}",
                        response.duration(), response.format(), response.sampleRate());
                return response;
            } catch (Exception e) {
                log.error("TTS 음성 합성 실패 - voice: {}, error: {}", request.voice(), e.getMessage(), e);
                ttsCallFailureCounter.increment();
                throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
            }
        });
    }

    /**
     * synthesize의 Fallback 메서드
     * 모든 재시도 실패 시 호출됨
     */
    public SynthesizeResponse recoverSynthesize(SynthesizeRequest request, Exception e) {
        log.error("TTS 음성 합성 실패 (모든 재시도 실패) - text length: {}", request.text().length(), e);
        ttsCallFailureCounter.increment();
        throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
    }

    /**
     * 텍스트를 음성으로 변환 (바이너리 응답)
     *
     * @param request 음성 합성 요청
     * @return 바이너리 오디오 데이터
     */
    @Retry(name = "ttsServiceRetry", fallbackMethod = "recoverSynthesizeBinary")
    public byte[] synthesizeBinary(SynthesizeRequest request) {
        log.debug("TTS 바이너리 음성 합성 시작 - text length: {}, voice: {}",
                request.text().length(), request.voice());

        // 텍스트 길이 검증
        if (request.text().length() > ttsConfig.getMaxTextLength()) {
            log.warn("TTS 텍스트 길이 초과 - length: {}, max: {}",
                    request.text().length(), ttsConfig.getMaxTextLength());
            throw new BusinessException(ErrorCode.TTS_TEXT_TOO_LONG);
        }

        return synthesizeTimer.record(() -> {
            try {
                // Base64 응답을 받아서 디코딩
                SynthesizeResponse response = synthesize(request);
                byte[] audioBytes = Base64.getDecoder().decode(response.audioData());

                log.info("TTS 바이너리 음성 합성 완료 - size: {} bytes", audioBytes.length);
                return audioBytes;
            } catch (BusinessException e) {
                throw e; // 이미 처리된 예외는 그대로 전파
            } catch (Exception e) {
                log.error("TTS 바이너리 음성 합성 실패 - error: {}", e.getMessage(), e);
                ttsCallFailureCounter.increment();
                throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
            }
        });
    }

    /**
     * synthesizeBinary의 Fallback 메서드
     */
    public byte[] recoverSynthesizeBinary(SynthesizeRequest request, Exception e) {
        log.error("TTS 바이너리 음성 합성 실패 (모든 재시도 실패) - text length: {}", request.text().length(), e);
        ttsCallFailureCounter.increment();
        throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
    }

    /**
     * 사용 가능한 음성 목록 조회
     *
     * @return 음성 정보 리스트
     */
    public List<VoiceInfo> getVoices() {
        log.debug("TTS 음성 목록 조회 시작");

        try {
            List<VoiceInfo> voices = restClient.get()
                    .uri("/api/voices")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<VoiceInfo>>() {});

            log.info("TTS 음성 목록 조회 완료 - count: {}", voices != null ? voices.size() : 0);
            return voices;
        } catch (Exception e) {
            log.error("TTS 음성 목록 조회 실패 - error: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
        }
    }

    /**
     * TTS 서비스 헬스체크
     *
     * @return 헬스체크 응답
     */
    public TtsHealthResponse health() {
        log.debug("TTS 헬스체크 시작");

        try {
            TtsHealthResponse response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(TtsHealthResponse.class);

            log.info("TTS 헬스체크 완료 - status: {}, modelLoaded: {}, device: {}",
                    response.status(), response.modelLoaded(), response.device());
            return response;
        } catch (Exception e) {
            log.error("TTS 헬스체크 실패 - error: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
        }
    }
}
