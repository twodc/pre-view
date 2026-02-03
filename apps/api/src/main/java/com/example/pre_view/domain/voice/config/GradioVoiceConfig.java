package com.example.pre_view.domain.voice.config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Gradio 음성 서비스 설정 (Kaggle 배포용)
 *
 * application.yaml의 gradio.voice.* 속성을 바인딩합니다.
 * 런타임에 URL과 활성화 상태를 동적으로 변경할 수 있습니다.
 */
@Slf4j
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

    // 동적 변경을 위한 AtomicReference
    private final AtomicReference<String> dynamicServiceUrl = new AtomicReference<>();
    private final AtomicBoolean dynamicEnabled = new AtomicBoolean(false);
    private volatile boolean initialized = false;

    /**
     * 동적 URL 가져오기 (런타임 변경 값 우선)
     */
    public String getEffectiveServiceUrl() {
        String dynamic = dynamicServiceUrl.get();
        return (dynamic != null && !dynamic.isBlank()) ? dynamic : serviceUrl;
    }

    /**
     * 동적 활성화 상태 가져오기 (런타임 변경 값 우선)
     */
    public boolean isEffectivelyEnabled() {
        if (!initialized) {
            dynamicEnabled.set(enabled);
            initialized = true;
        }
        return dynamicEnabled.get();
    }

    /**
     * 런타임에 URL 변경
     */
    public void setDynamicServiceUrl(String url) {
        log.info("Gradio 서버 URL 동적 변경: {} -> {}", dynamicServiceUrl.get(), url);
        dynamicServiceUrl.set(url);
    }

    /**
     * 런타임에 활성화 상태 변경
     */
    public void setDynamicEnabled(boolean enabled) {
        log.info("Gradio 서비스 활성화 상태 동적 변경: {}", enabled);
        dynamicEnabled.set(enabled);
        initialized = true;
    }

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
