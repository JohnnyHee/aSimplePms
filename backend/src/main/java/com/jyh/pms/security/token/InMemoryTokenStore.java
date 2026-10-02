package com.jyh.pms.security.token;

import com.jyh.pms.security.RefreshSession;
import com.jyh.pms.security.TokenStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版令牌存储：本地开发零依赖（不需要 Redis）。进程重启后所有会话失效。
 */
@Component
@ConditionalOnProperty(name = "pms.token-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, Entry<RefreshSession>> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();
    private final Map<String, Entry<AtomicLong>> loginFailures = new ConcurrentHashMap<>();

    @Override
    public void saveRefreshToken(RefreshSession session, Duration ttl) {
        purgeExpired();
        refreshTokens.put(session.jti(), new Entry<>(session, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<RefreshSession> findRefreshToken(String jti) {
        Entry<RefreshSession> entry = refreshTokens.get(jti);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expired()) {
            refreshTokens.remove(jti);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void revokeRefreshToken(String jti) {
        refreshTokens.remove(jti);
    }

    @Override
    public void blacklistAccessToken(String jti, Duration ttl) {
        if (jti == null) {
            return;
        }
        blacklist.put(jti, Instant.now().plus(ttl == null ? Duration.ofMinutes(30) : ttl));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiry = blacklist.get(jti);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    @Override
    public long recordLoginFailure(String username, Duration ttl) {
        Entry<AtomicLong> entry = loginFailures.compute(username, (key, current) -> {
            if (current == null || current.expired()) {
                return new Entry<>(new AtomicLong(0), Instant.now().plus(ttl));
            }
            current.value().incrementAndGet();
            return current;
        });
        return entry.value().get();
    }

    @Override
    public long loginFailureCount(String username) {
        Entry<AtomicLong> entry = loginFailures.get(username);
        if (entry == null || entry.expired()) {
            loginFailures.remove(username);
            return 0;
        }
        return entry.value().get();
    }

    @Override
    public void clearLoginFailures(String username) {
        loginFailures.remove(username);
    }

    @Override
    public int revokeAllForUser(String uid) {
        int count = 0;
        for (Map.Entry<String, Entry<RefreshSession>> entry : refreshTokens.entrySet()) {
            if (uid.equals(entry.getValue().value().uid())) {
                refreshTokens.remove(entry.getKey());
                count++;
            }
        }
        return count;
    }

    @Override
    public int activeSessions(String uid) {
        Set<String> uids = ConcurrentHashMap.newKeySet();
        refreshTokens.values().stream()
                .map(entry -> entry.value().uid())
                .filter(uid::equals)
                .forEach(uids::add);
        return uids.size();
    }

    @Override
    public void clear() {
        refreshTokens.clear();
        blacklist.clear();
        loginFailures.clear();
    }

    @Override
    public String implementation() {
        return "memory";
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        refreshTokens.entrySet().removeIf(entry -> entry.getValue().expiry().isBefore(now));
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        loginFailures.entrySet().removeIf(entry -> entry.getValue().expired());
    }

    /** 带过期时间的容器。 */
    private record Entry<T>(T value, Instant expiry) {
        boolean expired() {
            return expiry.isBefore(Instant.now());
        }
    }
}
