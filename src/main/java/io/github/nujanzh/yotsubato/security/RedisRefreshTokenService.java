package io.github.nujanzh.yotsubato.security;

import io.github.nujanzh.yotsubato.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisRefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisRefreshTokenService(
            StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.ttl = jwtProperties.refreshTokenTtl();
    }

    public void store(String token, UUID userId) {
        redisTemplate.opsForValue().set(key(token), userId.toString(), ttl);
    }

    public Optional<UUID> lookup(String token) {
        String value = redisTemplate.opsForValue().get(key(token));
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    public void delete(String token) {
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
