package com.example.pre_view.common.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "groq")
public class GroqConfig {
    private String apiKey;
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String primaryModel = "qwen/qwen3-32b";
    private String fallbackModel = "llama-3.3-70b-versatile";
    private double temperature = 0.5;
    private int maxTokens = 800;
    private int timeout = 60000;
}
