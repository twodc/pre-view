package com.example.pre_view.domain.config.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.pre_view.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * 프론트엔드에서 사용할 기능 플래그 API
 *
 * 환경변수에 따라 기능 활성화 여부를 반환합니다.
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Config", description = "앱 설정 API")
public class FeatureConfigController {

    @Value("${gradio.voice.enabled:false}")
    private boolean voiceEnabled;

    @Value("${gradio.voice.service-url:}")
    private String gradioUrl;

    @Operation(summary = "기능 설정 조회", description = "프론트엔드에서 사용할 기능 활성화 여부를 반환합니다.")
    @GetMapping("/features")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeatures() {
        Map<String, Object> features = Map.of(
            "voiceEnabled", voiceEnabled && !gradioUrl.isBlank(),
            "gradioConfigured", !gradioUrl.isBlank()
        );

        return ResponseEntity.ok(ApiResponse.ok(features));
    }
}
