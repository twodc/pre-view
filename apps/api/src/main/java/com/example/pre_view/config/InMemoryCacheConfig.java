package com.example.pre_view.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 인메모리 캐시 설정 (prod 프로필용)
 *
 * Redis 대신 ConcurrentMapCache 사용
 * 앱 재시작 시 데이터 초기화됨 (데모/테스트 용도)
 */
@Configuration
@EnableCaching
@Profile("prod")
public class InMemoryCacheConfig {

    /**
     * 인메모리 토큰 저장소 (Redis StringRedisTemplate 대체)
     */
    @Bean
    public ConcurrentHashMap<String, String> inMemoryTokenStore() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 인메모리 캐시 매니저 (@Cacheable 등 지원)
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("member", "interview", "interviewList");
    }
}
