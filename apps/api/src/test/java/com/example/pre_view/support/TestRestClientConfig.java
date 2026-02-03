package com.example.pre_view.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * 테스트용 RestClient 설정
 * TTS/STT Config에서 필요한 RestClient.Builder Bean을 제공
 */
@TestConfiguration
public class TestRestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
