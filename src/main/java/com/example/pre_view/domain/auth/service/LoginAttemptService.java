package com.example.pre_view.domain.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 시도 제한 서비스 (Brute Force 방지)
 *
 * Redis 저장 구조:
 * - Key: "login_attempt:{email}"
 * - Value: 시도 횟수 (String)
 * - TTL: 5분 (잠금 해제 시간)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login_attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    /**
     * 로그인 시도 횟수 증가
     *
     * @param email 로그인 시도한 이메일
     */
    public void recordFailedAttempt(String email) {
        String key = KEY_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(key);

        // 첫 번째 실패 시 TTL 설정
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, LOCK_DURATION);
        }

        log.debug("로그인 실패 기록 - email: {}, 시도 횟수: {}", email, attempts);
    }

    /**
     * 계정이 잠겨있는지 확인
     *
     * @param email 확인할 이메일
     * @return true면 잠금 상태
     */
    public boolean isLocked(String email) {
        String key = KEY_PREFIX + email;
        String attempts = redisTemplate.opsForValue().get(key);

        if (attempts == null) {
            return false;
        }

        return Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    /**
     * 로그인 성공 시 시도 횟수 초기화
     *
     * @param email 로그인 성공한 이메일
     */
    public void resetAttempts(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
        log.debug("로그인 시도 횟수 초기화 - email: {}", email);
    }

    /**
     * 남은 잠금 시간 조회 (초 단위)
     *
     * @param email 확인할 이메일
     * @return 남은 잠금 시간 (초), 잠금 상태가 아니면 0
     */
    public long getRemainingLockTime(String email) {
        String key = KEY_PREFIX + email;
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? ttl : 0;
    }
}
