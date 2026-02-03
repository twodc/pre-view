package com.example.pre_view.domain.voice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.domain.voice.dto.GradioSttResponse;
import com.example.pre_view.domain.voice.dto.GradioTtsResponse;
import com.example.pre_view.domain.voice.service.GradioVoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gradio 음성 서비스 컨트롤러
 *
 * Kaggle에 배포된 Gradio 앱과 연동하여 STT/TTS 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
@Tag(name = "Voice", description = "음성 서비스 API (STT/TTS)")
public class GradioVoiceController {

    private final GradioVoiceService gradioVoiceService;

    /**
     * 음성을 텍스트로 변환 (STT)
     */
    @PostMapping("/stt")
    @Operation(summary = "음성 인식 (STT)", description = "오디오 파일을 텍스트로 변환합니다.")
    public ResponseEntity<GradioSttResponse> transcribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "korean") String language) {

        log.info("STT 요청 - 파일: {}, 언어: {}", file.getOriginalFilename(), language);

        GradioSttResponse response = gradioVoiceService.transcribe(file, language);
        return ResponseEntity.ok(response);
    }

    /**
     * 텍스트를 음성으로 변환 (TTS)
     */
    @PostMapping("/tts")
    @Operation(summary = "음성 합성 (TTS)", description = "텍스트를 음성으로 변환합니다.")
    public ResponseEntity<GradioTtsResponse> synthesize(
            @RequestParam("text") String text,
            @RequestParam(value = "language", defaultValue = "Korean") String language) {

        log.info("TTS 요청 - 텍스트 길이: {}, 언어: {}", text.length(), language);

        GradioTtsResponse response = gradioVoiceService.synthesize(text, language);
        return ResponseEntity.ok(response);
    }

    /**
     * 음성 서비스 헬스체크
     */
    @GetMapping("/health")
    @Operation(summary = "헬스체크", description = "Gradio 음성 서비스 상태를 확인합니다.")
    public ResponseEntity<String> health() {
        boolean healthy = gradioVoiceService.isHealthy();
        if (healthy) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.status(503).body("Service Unavailable");
        }
    }
}
