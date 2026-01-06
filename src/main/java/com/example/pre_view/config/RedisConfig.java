package com.example.pre_view.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정
 *
 * StringRedisTemplate: key-value 모두 String으로 직렬화
 * - 토큰 저장에 적합 (단순 문자열)
 * - 디버깅 시 Redis CLI에서 값을 바로 확인 가능
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
