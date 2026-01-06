package com.example.pre_view.domain.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Access Token Blacklist 서비스 (RedisTemplate 방식)
 *
 * 로그아웃된 Access Token을 블랙리스트에 등록하여 재사용 방지
 *
 * Redis 저장 구조:
 * - Key: "blacklist:{accessToken}"
 * - Value: memberId (디버깅용)
 * - TTL: Access Token의 남은 유효시간
 *
 * 왜 TTL을 남은 유효시간으로 설정하는가?
 * → Access Token이 어차피 만료되면 Blacklist에 있을 필요 없음
 * → 자동 삭제로 Redis 메모리 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Access Token을 Blacklist에 등록
     *
     * @param accessToken 블랙리스트에 추가할 토큰
     * @param memberId 토큰 소유자 (디버깅용)
     * @param remainingTimeMs 토큰의 남은 유효시간 (밀리초)
     */
    public void add(String accessToken, String memberId, long remainingTimeMs) {
        String key = KEY_PREFIX + accessToken;
        Duration ttl = Duration.ofMillis(remainingTimeMs);

        redisTemplate.opsForValue().set(key, memberId, ttl);
        log.debug("Access Token 블랙리스트 등록 - memberId: {}, TTL: {}초", memberId, ttl.toSeconds());
    }

    /**
     * 토큰이 Blacklist에 있는지 확인
     *
     * @return true면 블랙리스트에 있음 → 접근 차단 필요
     */
    public boolean isBlacklisted(String accessToken) {
        String key = KEY_PREFIX + accessToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
