package com.example.pre_view.domain.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * LLM 서비스 설정
 *
 * application.yaml의 llm.* 속성을 바인딩합니다.
 * Python llm-service (SGLang + Qwen3-Instruct) 연동 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {

    /**
     * LLM 서비스 URL (기본값: http://localhost:8003)
     */
    private String serviceUrl = "http://localhost:8003";

    /**
     * LLM 서비스 타임아웃 (밀리초, 기본값: 60초)
     * LLM은 응답이 느릴 수 있어 타임아웃을 길게 설정
     */
    private int timeout = 60000;

    /**
     * LLM 서비스 사용 여부
     * false: 기존 Groq API 사용
     * true: 로컬 LLM 서비스 사용
     */
    private boolean enabled = false;
}
