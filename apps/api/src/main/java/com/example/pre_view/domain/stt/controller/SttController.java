package com.example.pre_view.domain.stt.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.dto.ApiResponse;
import com.example.pre_view.domain.auth.annotation.CurrentMemberId;
import com.example.pre_view.domain.stt.dto.SttHealthResponse;
import com.example.pre_view.domain.stt.dto.TranscriptionResponse;
import com.example.pre_view.domain.stt.service.SttService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STT API 컨트롤러
 *
 * 음성 인식(Speech-to-Text) 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stt")
@RequiredArgsConstructor
@Tag(name = "STT", description = "음성 인식 API")
public class SttController {

    private final SttService sttService;

    /**
     * 오디오 파일을 텍스트로 변환
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "음성을 텍스트로 변환", description = "오디오 파일을 업로드하여 텍스트로 변환합니다.")
    public ResponseEntity<ApiResponse<TranscriptionResponse>> transcribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "ko") String language,
            @CurrentMemberId Long memberId
    ) {
        log.info("STT 전사 API 호출 - memberId: {}, 파일명: {}, 크기: {} bytes, 언어: {}",
                memberId, file.getOriginalFilename(), file.getSize(), language);

        TranscriptionResponse response = sttService.transcribe(file, language);

        log.info("STT 전사 API 완료 - memberId: {}, 텍스트 길이: {}",
                memberId, response.text().length());

        return ResponseEntity.ok(ApiResponse.ok("음성이 텍스트로 변환되었습니다.", response));
    }

    /**
     * STT 서비스 헬스체크
     */
    @GetMapping("/health")
    @Operation(summary = "STT 서비스 상태 확인", description = "STT 서비스의 헬스체크를 수행합니다.")
    public ResponseEntity<ApiResponse<SttHealthResponse>> checkHealth() {
        log.info("STT 헬스체크 API 호출");

        SttHealthResponse response = sttService.checkHealth();

        log.info("STT 헬스체크 API 완료 - status: {}", response.status());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
