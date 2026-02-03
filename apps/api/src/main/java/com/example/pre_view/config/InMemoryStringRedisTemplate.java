package com.example.pre_view.config;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisOperations;
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
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final InMemoryValueOperations valueOperations = new InMemoryValueOperations();

    /**
     * Redis 연결 요구를 우회 - 인메모리 구현이므로 연결 불필요
     */
    @Override
    public void afterPropertiesSet() {
        // 부모 클래스의 afterPropertiesSet()을 호출하지 않음
        // RedisConnectionFactory 요구를 우회
    }

    @Override
    public ValueOperations<String, String> opsForValue() {
        return valueOperations;
    }

    @Override
    public Boolean delete(String key) {
        counters.remove(key);
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
            scheduler.schedule(() -> store.remove(key), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public String get(Object key) {
            return store.get(key);
        }

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
        public String setGet(String key, String value, Duration timeout) {
            String old = store.put(key, value);
            scheduler.schedule(() -> store.remove(key), timeout.toMillis(), TimeUnit.MILLISECONDS);
            return old;
        }

        @Override
        public String setGet(String key, String value, long timeout, TimeUnit unit) {
            return setGet(key, value, Duration.ofMillis(unit.toMillis(timeout)));
        }

        @Override
        public Long increment(String key) {
            AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
            long newValue = counter.incrementAndGet();
            store.put(key, String.valueOf(newValue));
            return newValue;
        }

        @Override
        public Long increment(String key, long delta) {
            AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
            long newValue = counter.addAndGet(delta);
            store.put(key, String.valueOf(newValue));
            return newValue;
        }

        @Override
        public Double increment(String key, double delta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decrement(String key) {
            AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
            long newValue = counter.decrementAndGet();
            store.put(key, String.valueOf(newValue));
            return newValue;
        }

        @Override
        public Long decrement(String key, long delta) {
            AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
            long newValue = counter.addAndGet(-delta);
            store.put(key, String.valueOf(newValue));
            return newValue;
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
        public List<Long> bitField(String key, BitFieldSubCommands subCommands) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RedisOperations<String, String> getOperations() {
            return InMemoryStringRedisTemplate.this;
        }

        @Override
        public List<String> multiGet(Collection<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void multiSet(Map<? extends String, ? extends String> map) {
            store.putAll(map);
        }

        @Override
        public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) {
            throw new UnsupportedOperationException();
        }
    }
}
