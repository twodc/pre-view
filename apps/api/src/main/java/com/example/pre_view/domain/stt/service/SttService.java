package com.example.pre_view.domain.stt.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.stt.config.SttConfig;
import com.example.pre_view.domain.stt.dto.SttHealthResponse;
import com.example.pre_view.domain.stt.dto.TranscriptionResponse;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STT 서비스
 *
 * Python stt-service와 통신하여 음성 인식 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

    private final RestClient sttRestClient;
    private final SttConfig sttConfig;

    /**
     * 오디오 파일을 텍스트로 변환
     *
     * @param audioFile 오디오 파일 (MP3, WAV, M4A 등)
     * @param language 언어 코드 (ko, en 등)
     * @return 전사 결과
     */
    @Retry(name = "sttServiceRetry")
    public TranscriptionResponse transcribe(@NonNull MultipartFile audioFile, @NonNull String language) {
        log.info("STT 전사 요청 시작 - 파일명: {}, 크기: {} bytes, 언어: {}",
                audioFile.getOriginalFilename(), audioFile.getSize(), language);

        // 파일 크기 검증
        validateAudioSize(audioFile);

        try {
            // MultipartBodyBuilder로 multipart/form-data 구성
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            });
            builder.part("language", language);

            // STT 서비스 호출
            TranscriptionResponse response = sttRestClient.post()
                    .uri("/api/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(TranscriptionResponse.class);

            if (response == null) {
                log.error("STT 서비스 응답이 null입니다.");
                throw new BusinessException(ErrorCode.STT_SERVICE_ERROR);
            }

            // 오디오 길이 검증 (응답 후)
            validateAudioDuration(response.duration());

            log.info("STT 전사 완료 - 텍스트 길이: {}, 신뢰도: {}, 오디오 길이: {}초",
                    response.text().length(), response.confidence(), response.duration());

            return response;

        } catch (IOException e) {
            log.error("오디오 파일 읽기 실패 - 파일명: {}", audioFile.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("STT 서비스 호출 실패", e);
            throw new BusinessException(ErrorCode.STT_SERVICE_ERROR);
        }
    }

    /**
     * STT 서비스 헬스체크
     *
     * @return 서비스 상태
     */
    public SttHealthResponse checkHealth() {
        log.debug("STT 서비스 헬스체크 시작");

        try {
            SttHealthResponse response = sttRestClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(SttHealthResponse.class);

            if (response == null) {
                log.error("STT 헬스체크 응답이 null입니다.");
                throw new BusinessException(ErrorCode.STT_SERVICE_ERROR);
            }

            log.info("STT 서비스 헬스체크 완료 - status: {}, modelLoaded: {}, device: {}",
                    response.status(), response.modelLoaded(), response.device());

            return response;

        } catch (Exception e) {
            log.error("STT 서비스 헬스체크 실패", e);
            throw new BusinessException(ErrorCode.STT_SERVICE_ERROR);
        }
    }

    /**
     * 오디오 파일 크기 검증
     */
    private void validateAudioSize(MultipartFile audioFile) {
        if (audioFile.getSize() > sttConfig.getMaxAudioSize()) {
            log.warn("오디오 파일 크기 초과 - 파일명: {}, 크기: {} bytes, 최대: {} bytes",
                    audioFile.getOriginalFilename(), audioFile.getSize(), sttConfig.getMaxAudioSize());
            throw new BusinessException(ErrorCode.STT_AUDIO_TOO_LARGE);
        }
    }

    /**
     * 오디오 길이 검증 (초)
     */
    private void validateAudioDuration(Double duration) {
        if (duration != null && duration > sttConfig.getMaxAudioDuration()) {
            log.warn("오디오 길이 초과 - 길이: {}초, 최대: {}초",
                    duration, sttConfig.getMaxAudioDuration());
            throw new BusinessException(ErrorCode.STT_AUDIO_TOO_LONG);
        }
    }
}
