package com.example.pre_view.domain.voice.service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.voice.config.GradioVoiceConfig;
import com.example.pre_view.domain.voice.dto.GradioSttResponse;
import com.example.pre_view.domain.voice.dto.GradioTtsResponse;
import com.example.pre_view.domain.voice.dto.VoiceServerStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gradio 음성 서비스 (Kaggle 배포 연동)
 *
 * Kaggle에 배포된 Gradio 앱의 STT/TTS API를 호출합니다.
 * - STT: Whisper-large-v3 (음성 → 텍스트)
 * - TTS: Qwen3-TTS (텍스트 → 음성)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradioVoiceService {

    private final RestClient gradioRestClient;
    private final GradioVoiceConfig config;
    private final JsonMapper objectMapper;

    /**
     * 음성을 텍스트로 변환 (STT)
     *
     * @param audioFile 오디오 파일
     * @param language  언어 (korean, english, japanese)
     * @return 전사된 텍스트
     */
    @Retry(name = "sttServiceRetry")
    public GradioSttResponse transcribe(MultipartFile audioFile, String language) {
        log.info("Gradio STT 요청 - 파일: {}, 언어: {}", audioFile.getOriginalFilename(), language);

        if (!config.isEffectivelyEnabled()) {
            log.info("Gradio 음성 서비스 비활성화 - Mock 응답 반환");
            return new GradioSttResponse("[음성 인식 테스트] 이것은 mock 응답입니다.", language, 0.95);
        }

        try {
            // Gradio API 호출 (POST /call/transcribe)
            // Step 1: 파일 업로드 및 호출 시작
            byte[] audioBytes = audioFile.getBytes();
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

            // MIME type 결정 (파일 확장자 기반)
            String filename = audioFile.getOriginalFilename();
            String mimeType = "audio/wav";  // 기본값
            if (filename != null) {
                if (filename.endsWith(".ogg") || filename.endsWith(".webm")) {
                    mimeType = "audio/ogg";
                } else if (filename.endsWith(".mp3")) {
                    mimeType = "audio/mpeg";
                } else if (filename.endsWith(".m4a")) {
                    mimeType = "audio/mp4";
                }
            }

            Map<String, Object> requestBody = Map.of(
                "data", List.of(
                    Map.of(
                        "name", filename,
                        "data", "data:" + mimeType + ";base64," + audioBase64
                    ),
                    language
                )
            );

            RestClient client = createDynamicRestClient();

            String response = client.post()
                    .uri("/gradio_api/call/transcribe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("Gradio STT 호출 응답: {}", response);

            // Step 2: event_id로 결과 가져오기
            JsonNode responseNode = objectMapper.readTree(response);
            String eventId = responseNode.get("event_id").asText();

            String result = client.get()
                    .uri("/gradio_api/call/transcribe/" + eventId)
                    .retrieve()
                    .body(String.class);

            log.debug("Gradio STT 결과: {}", result);

            // 결과 파싱 (SSE 형식: event: complete\ndata: [...])
            String text = parseGradioResult(result);

            log.info("Gradio STT 완료 - 텍스트 길이: {}", text.length());
            return new GradioSttResponse(text, language, null);

        } catch (IOException e) {
            log.error("Gradio STT 실패 - 파일 읽기 오류", e);
            throw new BusinessException(ErrorCode.STT_SERVICE_ERROR);
        } catch (Exception e) {
            log.error("Gradio STT 실패", e);
            throw new BusinessException(ErrorCode.STT_SERVICE_ERROR);
        }
    }

    /**
     * 텍스트를 음성으로 변환 (TTS)
     *
     * @param text     변환할 텍스트
     * @param language 언어 (Korean, English, Japanese)
     * @return 오디오 데이터 (Base64)
     */
    @Retry(name = "ttsServiceRetry")
    public GradioTtsResponse synthesize(String text, String language) {
        log.info("Gradio TTS 요청 - 텍스트 길이: {}, 언어: {}", text.length(), language);

        if (!config.isEffectivelyEnabled()) {
            log.info("Gradio 음성 서비스 비활성화 - Mock 응답 반환 (Web Speech API fallback 사용)");
            return new GradioTtsResponse(null, "wav", 24000);  // null → 프론트엔드에서 Web Speech API fallback
        }

        try {
            RestClient client = createDynamicRestClient();

            // Gradio API 호출 (POST /call/synthesize)
            Map<String, Object> requestBody = Map.of(
                "data", List.of(text, language)
            );

            String response = client.post()
                    .uri("/gradio_api/call/synthesize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("Gradio TTS 호출 응답: {}", response);

            // event_id로 결과 가져오기
            JsonNode responseNode = objectMapper.readTree(response);
            String eventId = responseNode.get("event_id").asText();

            String result = client.get()
                    .uri("/gradio_api/call/synthesize/" + eventId)
                    .retrieve()
                    .body(String.class);

            log.debug("Gradio TTS 결과: {}", result);

            // 결과 파싱 (오디오 파일 경로 또는 Base64)
            String audioData = parseGradioAudioResult(result);

            log.info("Gradio TTS 완료");
            return new GradioTtsResponse(audioData, "wav", 24000);

        } catch (Exception e) {
            log.error("Gradio TTS 실패", e);
            throw new BusinessException(ErrorCode.TTS_SERVICE_ERROR);
        }
    }

    /**
     * Gradio SSE 응답 파싱 (텍스트 결과)
     */
    private String parseGradioResult(String sseResponse) {
        try {
            // SSE 형식: event: complete\ndata: ["결과텍스트"]
            String[] lines = sseResponse.split("\n");
            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    JsonNode dataNode = objectMapper.readTree(jsonData);
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        return dataNode.get(0).asText();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("Gradio 응답 파싱 실패: {}", sseResponse, e);
            return sseResponse;
        }
    }

    /**
     * Gradio SSE 응답 파싱 (오디오 결과)
     */
    private String parseGradioAudioResult(String sseResponse) {
        try {
            // SSE 형식: event: complete\ndata: [{"path": "/tmp/xxx.wav", ...}]
            String[] lines = sseResponse.split("\n");
            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    JsonNode dataNode = objectMapper.readTree(jsonData);
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        JsonNode audioNode = dataNode.get(0);
                        if (audioNode.has("url")) {
                            // URL로 오디오 다운로드
                            return downloadAudioAsBase64(audioNode.get("url").asText());
                        } else if (audioNode.has("path")) {
                            return audioNode.get("path").asText();
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("Gradio 오디오 응답 파싱 실패: {}", sseResponse, e);
            return "";
        }
    }

    /**
     * 오디오 URL에서 다운로드 후 Base64 인코딩
     */
    private String downloadAudioAsBase64(String url) {
        try {
            RestClient client = createDynamicRestClient();
            byte[] audioBytes = client.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);

            return Base64.getEncoder().encodeToString(audioBytes);
        } catch (Exception e) {
            log.error("오디오 다운로드 실패: {}", url, e);
            return "";
        }
    }

    /**
     * Gradio 서비스 헬스체크
     */
    public boolean isHealthy() {
        if (!config.isEffectivelyEnabled()) {
            return false;
        }

        try {
            RestClient client = createDynamicRestClient();
            String response = client.get()
                    .uri("/")
                    .retrieve()
                    .body(String.class);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.warn("Gradio 헬스체크 실패", e);
            return false;
        }
    }

    /**
     * 현재 서버 상태 조회
     */
    public VoiceServerStatus getStatus() {
        String url = config.getEffectiveServiceUrl();
        boolean enabled = config.isEffectivelyEnabled();
        boolean available = enabled && url != null && !url.isBlank();

        boolean healthy = false;
        String message = "음성 서버가 설정되지 않았습니다.";

        if (available) {
            try {
                healthy = isHealthy();
                message = healthy ? "정상 연결됨" : "연결 실패";
            } catch (Exception e) {
                message = "연결 실패: " + e.getMessage();
            }
        } else if (!enabled) {
            message = "음성 서버가 비활성화되어 있습니다.";
        }

        return new VoiceServerStatus(enabled, url, available, healthy, message);
    }

    /**
     * 동적 URL을 사용하는 RestClient 생성
     */
    private RestClient createDynamicRestClient() {
        String effectiveUrl = config.getEffectiveServiceUrl();
        if (effectiveUrl == null || effectiveUrl.isBlank()) {
            effectiveUrl = "http://localhost:7860";
        }
        return RestClient.builder()
                .baseUrl(effectiveUrl)
                .build();
    }
}
