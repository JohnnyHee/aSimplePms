package com.jyh.pms.security.token;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jyh.pms.security.RedisKeys;
import com.jyh.pms.security.RefreshSession;
import com.jyh.pms.security.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 版令牌存储（生产使用）：{@code pms.token-store=redis}。
 * 支持多实例部署、刷新令牌轮换、访问令牌黑名单与"改密即下线"。
 */
@Component
@ConditionalOnProperty(name = "pms.token-store", havingValue = "redis")
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisTokenStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveRefreshToken(RefreshSession session, Duration ttl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jti", session.jti());
        payload.put("uid", session.uid());
        payload.put("username", session.username());
        payload.put("issuedAt", session.issuedAt().toEpochMilli());
        payload.put("expiresAt", session.expiresAt().toEpochMilli());
        try {
            redis.opsForValue().set(RedisKeys.refresh(session.jti()), objectMapper.writeValueAsString(payload), ttl);
            redis.opsForSet().add(RedisKeys.userSessions(session.uid()), session.jti());
            redis.expire(RedisKeys.userSessions(session.uid()), ttl);
        } catch (Exception e) {
            log.error("写入刷新令牌失败: {}", e.getMessage());
        }
    }

    @Override
    public Optional<RefreshSession> findRefreshToken(String jti) {
        String json = redis.opsForValue().get(RedisKeys.refresh(jti));
        if (json == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {
            });
            return Optional.of(new RefreshSession(
                    String.valueOf(payload.get("jti")),
                    String.valueOf(payload.get("uid")),
                    String.valueOf(payload.get("username")),
                    Instant.ofEpochMilli(((Number) payload.get("issuedAt")).longValue()),
                    Instant.ofEpochMilli(((Number) payload.get("expiresAt")).longValue())));
        } catch (Exception e) {
            log.warn("解析刷新令牌失败, jti={}, 原因={}", jti, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void revokeRefreshToken(String jti) {
        Optional<RefreshSession> session = findRefreshToken(jti);
        redis.delete(RedisKeys.refresh(jti));
        session.ifPresent(value -> redis.opsForSet().remove(RedisKeys.userSessions(value.uid()), jti));
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (jti == null) {
            return;
        }
        Duration effective = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofMinutes(30) : ttl;
        redis.opsForValue().set(RedisKeys.blacklist(jti), "1", effective);
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(RedisKeys.blacklist(jti)));
    }

    @Override
    public long recordLoginFailure(String username, Duration ttl) {
        String key = RedisKeys.loginFail(username);
        Long count = redis.opsForValue().increment(key);
        redis.expire(key, ttl);
        return count == null ? 1L : count;
    }

    @Override
    public long loginFailureCount(String username) {
        String value = redis.opsForValue().get(RedisKeys.loginFail(username));
        return value == null ? 0L : Long.parseLong(value);
    }

    @Override
    public void clearLoginFailures(String username) {
        redis.delete(RedisKeys.loginFail(username));
    }

    @Override
    public int revokeAllForUser(String uid) {
        Set<String> jtis = redis.opsForSet().members(RedisKeys.userSessions(uid));
        if (jtis == null || jtis.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String jti : jtis) {
            Optional<RefreshSession> session = findRefreshToken(jti);
            if (session.isPresent()) {
                long remainSeconds = Math.max(0,
                        session.get().expiresAt().getEpochSecond() - Instant.now().getEpochSecond());
                if (remainSeconds > 0) {
                    redis.opsForValue().set(RedisKeys.blacklist(jti), "1", remainSeconds, TimeUnit.SECONDS);
                }
            }
            redis.delete(RedisKeys.refresh(jti));
            count++;
        }
        redis.delete(RedisKeys.userSessions(uid));
        return count;
    }

    @Override
    public int activeSessions(String uid) {
        Long size = redis.opsForSet().size(RedisKeys.userSessions(uid));
        return size == null ? 0 : size.intValue();
    }

    @Override
    public void clear() {
        List<String> prefixes = List.of(RedisKeys.REFRESH_PREFIX, RedisKeys.BLACKLIST_PREFIX,
                RedisKeys.USER_SESSIONS_PREFIX, RedisKeys.LOGIN_FAIL_PREFIX);
        for (String prefix : prefixes) {
            Set<String> keys = redis.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        }
    }

    @Override
    public String implementation() {
        return "redis";
    }
}
