package com.example.pre_view.domain.stt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.Getter;
import lombok.Setter;

/**
 * STT 서비스 설정
 *
 * application.yaml의 stt.* 속성을 바인딩합니다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "stt")
public class SttConfig {

    /**
     * STT 서비스 URL (예: http://localhost:8001)
     */
    private String serviceUrl;

    /**
     * 타임아웃 (밀리초)
     */
    private int timeout;

    /**
     * 최대 오디오 파일 크기 (바이트)
     */
    private long maxAudioSize;

    /**
     * 최대 오디오 길이 (초)
     */
    private int maxAudioDuration;

    /**
     * STT 서비스용 RestClient 빈 생성
     */
    @Bean
    public RestClient sttRestClient() {
        return RestClient.builder()
                .baseUrl(serviceUrl != null ? serviceUrl : "http://localhost:8001")
                .build();
    }
}
