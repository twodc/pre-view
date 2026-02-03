package com.example.pre_view.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * 인메모리 StringRedisTemplate 대체 구현 (prod 프로필용)
 *
 * Redis 없이 ConcurrentHashMap으로 토큰 저장
 * TTL은 ScheduledExecutor로 만료 처리
 */
@Component
@Profile("prod")
public class InMemoryStringRedisTemplate extends StringRedisTemplate {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final InMemoryValueOperations valueOperations = new InMemoryValueOperations();

    @Override
    public ValueOperations<String, String> opsForValue() {
        return valueOperations;
    }

    @Override
    public Boolean delete(String key) {
        return store.remove(key) != null;
    }

    private class InMemoryValueOperations implements ValueOperations<String, String> {

        @Override
        public void set(String key, String value) {
            store.put(key, value);
        }

        @Override
        public void set(String key, String value, Duration timeout) {
            store.put(key, value);
            // TTL 후 자동 삭제
            scheduler.schedule(() -> store.remove(key), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public String get(Object key) {
            return store.get(key);
        }

        // 사용하지 않는 메서드들 - 기본 구현
        @Override
        public void set(String key, String value, long timeout, TimeUnit unit) {
            set(key, value, Duration.ofMillis(unit.toMillis(timeout)));
        }

        @Override
        public Boolean setIfAbsent(String key, String value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, Duration timeout) {
            if (store.putIfAbsent(key, value) == null) {
                scheduler.schedule(() -> store.remove(key), timeout.toMillis(), TimeUnit.MILLISECONDS);
                return true;
            }
            return false;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
            return setIfAbsent(key, value, Duration.ofMillis(unit.toMillis(timeout)));
        }

        @Override
        public Boolean setIfPresent(String key, String value) {
            return store.replace(key, value) != null;
        }

        @Override
        public Boolean setIfPresent(String key, String value, Duration timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndDelete(String key) {
            return store.remove(key);
        }

        @Override
        public String getAndExpire(String key, Duration timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndExpire(String key, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndPersist(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAndSet(String key, String value) {
            return store.put(key, value);
        }

        @Override
        public Long increment(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long increment(String key, long delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Double increment(String key, double delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decrement(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decrement(String key, long delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer append(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(String key, long start, long end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(String key, String value, long offset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long size(String key) {
            String val = store.get(key);
            return val != null ? (long) val.length() : 0L;
        }

        @Override
        public Boolean setBit(String key, long offset, boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean getBit(String key, long offset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<Long> bitField(String key, org.springframework.data.redis.core.BitFieldSubCommands subCommands) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.springframework.data.redis.core.RedisOperations<String, String> getOperations() {
            return InMemoryStringRedisTemplate.this;
        }

        @Override
        public java.util.List<String> multiGet(java.util.Collection<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void multiSet(java.util.Map<? extends String, ? extends String> map) {
            store.putAll(map);
        }

        @Override
        public Boolean multiSetIfAbsent(java.util.Map<? extends String, ? extends String> map) {
            throw new UnsupportedOperationException();
        }
    }
}
