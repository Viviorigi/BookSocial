package com.duong.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class VerifyCooldownService {
    private final StringRedisTemplate redis;
    private static final String KEY_FMT = "verify:cooldown:%s"; // theo userId

    /** true = được phép; false = đang cooldown */
    public boolean tryAcquire(String userId, Duration cooldown) {
        String key = KEY_FMT.formatted(userId);
        // SET key NX EX=<cooldown>  (setIfAbsent + TTL)
        Boolean ok = redis.opsForValue().setIfAbsent(key, "1", cooldown);
        return Boolean.TRUE.equals(ok);
    }
}
