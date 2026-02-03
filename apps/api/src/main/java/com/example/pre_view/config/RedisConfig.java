package com.example.pre_view.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis 설정
 *
 * 1. StringRedisTemplate: 토큰 저장용 (key-value 모두 String)
 * 2. CacheManager: Spring Cache 추상화 지원 (@Cacheable 등)
 */
@Configuration
@EnableCaching
@Profile("!prod")
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Redis 캐시 매니저 설정
     * - 기본 TTL: 10분
     * - Null 값 캐시 비허용 (cache miss 시 매번 조회)
     * - JSON 직렬화 (타입 정보 포함 - 역직렬화 시 올바른 타입 복원)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper 설정 (타입 정보 포함)
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
                .build();

        // GenericJackson2JsonRedisSerializer 사용 (타입 정보 자동 포함)
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // 캐시별 TTL 커스터마이징
                .withCacheConfiguration("member",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))  // 회원 정보: 30분
                .withCacheConfiguration("interview",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))   // 면접 정보: 5분
                .withCacheConfiguration("interviewList",
                        defaultConfig.entryTtl(Duration.ofMinutes(2)))   // 면접 목록: 2분
                .build();
    }
}
