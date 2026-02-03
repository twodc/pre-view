package com.example.pre_view.domain.tts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * TTS 서비스 설정
 *
 * application.yaml의 tts.* 속성을 바인딩합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tts")
public class TtsConfig {

    /**
     * TTS 서비스 URL (기본값: http://localhost:8002)
     */
    private String serviceUrl = "http://localhost:8002";

    /**
     * TTS 서비스 타임아웃 (밀리초, 기본값: 30초)
     */
    private int timeout = 30000;

    /**
     * 최대 텍스트 길이 (기본값: 2000자)
     */
    private int maxTextLength = 2000;

    /**
     * 기본 음성 스타일 (기본값: female_calm)
     */
    private String defaultVoice = "female_calm";
}
