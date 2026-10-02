package com.jyh.pms.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code pms.*} 自定义配置。
 */
@ConfigurationProperties(prefix = "pms")
public class PmsProperties {

    /** 数据存储实现：memory / mongodb。 */
    private String storage = "memory";
    /** 缓存实现：memory / redis。 */
    private String cache = "memory";
    /** 令牌存储实现：memory / redis。 */
    private String tokenStore = "memory";

    private final Security security = new Security();
    private final Cors cors = new Cors();
    private final Init init = new Init();

    public boolean useMongoStorage() {
        return "mongodb".equalsIgnoreCase(storage);
    }

    public boolean useRedisCache() {
        return "redis".equalsIgnoreCase(cache);
    }

    public boolean useRedisTokenStore() {
        return "redis".equalsIgnoreCase(tokenStore);
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(String tokenStore) {
        this.tokenStore = tokenStore;
    }

    public Security getSecurity() {
        return security;
    }

    public Cors getCors() {
        return cors;
    }

    public Init getInit() {
        return init;
    }

    public static class Security {
        /** HMAC-SHA256 密钥，长度至少 32 字节。 */
        private String jwtSecret = "pms-dev-only-secret-please-override-in-production-0123456789";
        private String issuer = "a-simple-pms";
        private Duration accessTokenTtl = Duration.ofMinutes(30);
        private Duration refreshTokenTtl = Duration.ofDays(7);
        private int maxLoginFailures = 5;
        private Duration lockDuration = Duration.ofMinutes(10);

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }

        public int getMaxLoginFailures() {
            return maxLoginFailures;
        }

        public void setMaxLoginFailures(int maxLoginFailures) {
            this.maxLoginFailures = maxLoginFailures;
        }

        public Duration getLockDuration() {
            return lockDuration;
        }

        public void setLockDuration(Duration lockDuration) {
            this.lockDuration = lockDuration;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Init {
        private boolean enabled = true;
        private String adminUsername = "admin";
        private String adminPassword = "Admin@123";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }
    }
}
