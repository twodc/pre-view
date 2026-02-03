package com.example.pre_view.domain.tts.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.tts.dto.SynthesizeRequest;
import com.example.pre_view.domain.tts.dto.SynthesizeResponse;
import com.example.pre_view.domain.tts.dto.TtsHealthResponse;
import com.example.pre_view.domain.tts.dto.VoiceInfo;
import com.example.pre_view.domain.tts.service.TtsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TTS API 컨트롤러
 *
 * 텍스트를 음성으로 변환하는 TTS(Text-to-Speech) 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
@Tag(name = "TTS", description = "음성 합성 API")
public class TtsController {

    private final TtsService ttsService;

    @PostMapping("/synthesize")
    @Operation(summary = "음성 합성 (Base64)", description = "텍스트를 음성으로 변환하고 Base64로 인코딩하여 반환합니다.")
    public ResponseEntity<ApiResponse<SynthesizeResponse>> synthesize(
            @Valid @RequestBody SynthesizeRequest request
    ) {
        log.info("음성 합성 API 호출 - text length: {}, voice: {}, speed: {}, format: {}",
                request.text().length(), request.voice(), request.speed(), request.format());

        SynthesizeResponse response = ttsService.synthesize(request);

        log.info("음성 합성 완료 - duration: {}s, format: {}", response.duration(), response.format());
        return ResponseEntity.ok(ApiResponse.ok("음성 합성이 완료되었습니다.", response));
    }

    @PostMapping("/synthesize/binary")
    @Operation(summary = "음성 합성 (바이너리)", description = "텍스트를 음성으로 변환하고 바이너리로 반환합니다.")
    public ResponseEntity<byte[]> synthesizeBinary(
            @Valid @RequestBody SynthesizeRequest request
    ) {
        log.info("바이너리 음성 합성 API 호출 - text length: {}, voice: {}, format: {}",
                request.text().length(), request.voice(), request.format());

        byte[] audioData = ttsService.synthesizeBinary(request);

        // Content-Type 설정 (wav 또는 mp3)
        String contentType = "wav".equals(request.format())
                ? "audio/wav"
                : "audio/mpeg";

        log.info("바이너리 음성 합성 완료 - size: {} bytes, format: {}", audioData.length, request.format());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech." + request.format() + "\"")
                .body(audioData);
    }

    @GetMapping("/voices")
    @Operation(summary = "음성 목록 조회", description = "사용 가능한 음성 스타일 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<VoiceInfo>>> getVoices() {
        log.info("음성 목록 조회 API 호출");

        List<VoiceInfo> voices = ttsService.getVoices();

        log.info("음성 목록 조회 완료 - count: {}", voices.size());
        return ResponseEntity.ok(ApiResponse.ok(voices));
    }

    @GetMapping("/health")
    @Operation(summary = "TTS 서비스 헬스체크", description = "TTS 서비스의 상태를 확인합니다.")
    public ResponseEntity<ApiResponse<TtsHealthResponse>> health() {
        log.info("TTS 헬스체크 API 호출");

        TtsHealthResponse response = ttsService.health();

        log.info("TTS 헬스체크 완료 - status: {}", response.status());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
