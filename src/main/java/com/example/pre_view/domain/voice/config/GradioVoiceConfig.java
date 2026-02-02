package com.example.pre_view.domain.voice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.Getter;
import lombok.Setter;

/**
 * Gradio 음성 서비스 설정 (Kaggle 배포용)
 *
 * application.yaml의 gradio.voice.* 속성을 바인딩합니다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gradio.voice")
public class GradioVoiceConfig {

    /**
     * Gradio 서비스 URL (예: https://xxxxx.gradio.live)
     */
    private String serviceUrl;

    /**
     * 타임아웃 (밀리초, 기본값: 60초)
     */
    private int timeout = 60000;

    /**
     * 활성화 여부 (기본값: false)
     */
    private boolean enabled = false;

    /**
     * Gradio 서비스용 RestClient 빈 생성
     */
    @Bean
    public RestClient gradioRestClient() {
        return RestClient.builder()
                .baseUrl(serviceUrl != null ? serviceUrl : "http://localhost:7860")
                .build();
    }
}
