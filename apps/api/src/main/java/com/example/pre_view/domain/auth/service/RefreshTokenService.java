package com.example.pre_view.domain.auth.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token 저장소 (RedisTemplate 방식)
 *
 * Redis 저장 구조:
 * - Key: "refresh:{memberId}"
 * - Value: JWT 토큰 문자열
 * - TTL: 7일 (application.yaml에서 설정)
 *
 * Repository 방식 대비 장점:
 * 1. 메모리 효율: 단순 key-value로 저장 (Hash + Set 오버헤드 없음)
 * 2. 명시적 제어: Redis 명령어가 어떻게 실행되는지 명확
 * 3. 성능: 불필요한 보조 인덱스 생성 없음
 */
@Slf4j
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshExpiration;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.redisTemplate = redisTemplate;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Refresh Token 저장
     * 기존 토큰이 있으면 덮어쓰기 (자동 Rotation)
     */
    public void save(String memberId, String token) {
        String key = KEY_PREFIX + memberId;
        // 밀리초 → Duration 변환
        Duration ttl = Duration.ofMillis(refreshExpiration);

        redisTemplate.opsForValue().set(key, token, ttl);
        log.debug("Refresh Token 저장 - memberId: {}, TTL: {}일", memberId, ttl.toDays());
    }

    /**
     * memberId로 Refresh Token 조회
     */
    public Optional<String> findByMemberId(String memberId) {
        String key = KEY_PREFIX + memberId;
        String token = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    /**
     * Refresh Token 삭제 (로그아웃 시)
     */
    public void delete(String memberId) {
        String key = KEY_PREFIX + memberId;
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Refresh Token 삭제 - memberId: {}, 결과: {}", memberId, deleted);
    }

    /**
     * 토큰 값이 저장된 토큰과 일치하는지 검증
     * Token Rotation 시 탈취된 이전 토큰 사용 방지
     */
    public boolean validateToken(String memberId, String token) {
        return findByMemberId(memberId)
                .map(savedToken -> savedToken.equals(token))
                .orElse(false);
    }
}
