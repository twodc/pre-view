package com.example.pre_view.domain.voice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.voice.config.GradioVoiceConfig;
import com.example.pre_view.domain.voice.dto.VoiceServerStatus;
import com.example.pre_view.domain.voice.dto.VoiceServerUpdateRequest;
import com.example.pre_view.domain.voice.service.GradioVoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 음성 서버 설정 API
 *
 * Kaggle/로컬 음성 서버 URL을 동적으로 설정합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/voice")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Voice", description = "관리자 음성 서버 설정 API")
public class AdminVoiceController {

    private final GradioVoiceService gradioVoiceService;
    private final GradioVoiceConfig gradioVoiceConfig;

    @GetMapping("/status")
    @Operation(summary = "음성 서버 상태 조회", description = "현재 음성 서버 연결 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<VoiceServerStatus>> getStatus() {
        log.info("음성 서버 상태 조회 요청");

        VoiceServerStatus status = gradioVoiceService.getStatus();

        log.info("음성 서버 상태: enabled={}, healthy={}", status.enabled(), status.healthy());
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/config")
    @Operation(summary = "음성 서버 설정 변경", description = "Gradio 서버 URL을 설정하거나 음성 기능을 활성화/비활성화합니다.")
    public ResponseEntity<ApiResponse<VoiceServerStatus>> updateConfig(
            @Valid @RequestBody VoiceServerUpdateRequest request
    ) {
        log.info("음성 서버 설정 변경 요청 - enabled: {}, url: {}",
                request.enabled(), request.gradioUrl());

        // URL 업데이트
        if (request.gradioUrl() != null) {
            gradioVoiceConfig.setDynamicServiceUrl(request.gradioUrl());
        }

        // 활성화 상태 업데이트
        if (request.enabled() != null) {
            gradioVoiceConfig.setDynamicEnabled(request.enabled());
        }

        VoiceServerStatus status = gradioVoiceService.getStatus();

        log.info("음성 서버 설정 변경 완료 - enabled: {}, healthy: {}",
                status.enabled(), status.healthy());

        return ResponseEntity.ok(ApiResponse.ok("음성 서버 설정이 변경되었습니다.", status));
    }

    @PostMapping("/health-check")
    @Operation(summary = "음성 서버 헬스체크", description = "설정된 Gradio 서버의 연결 상태를 확인합니다.")
    public ResponseEntity<ApiResponse<VoiceServerStatus>> healthCheck() {
        log.info("음성 서버 헬스체크 요청");

        VoiceServerStatus status = gradioVoiceService.getStatus();

        String message = status.healthy()
                ? "음성 서버가 정상 연결되었습니다."
                : "음성 서버 연결에 실패했습니다.";

        log.info("음성 서버 헬스체크 완료 - healthy: {}", status.healthy());
        return ResponseEntity.ok(ApiResponse.ok(message, status));
    }
}
