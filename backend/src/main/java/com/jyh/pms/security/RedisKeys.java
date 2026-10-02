package com.jyh.pms.security;

/**
 * Redis 键位统一约定，避免旧项目里 {@code set("user", json)} 那种随手起名的键。
 */
public final class RedisKeys {

    /** 刷新令牌：pms:auth:refresh:{jti} -> RefreshSession(json) */
    public static final String REFRESH_PREFIX = "pms:auth:refresh:";

    /** 访问令牌黑名单：pms:auth:blacklist:{jti} -> 1 */
    public static final String BLACKLIST_PREFIX = "pms:auth:blacklist:";

    /** 用户维度会话集合：pms:auth:user:{uid} -> Set<jti> */
    public static final String USER_SESSIONS_PREFIX = "pms:auth:user:";

    /** 登录失败计数：pms:auth:fail:{username} -> 次数 */
    public static final String LOGIN_FAIL_PREFIX = "pms:auth:fail:";

    /** 业务缓存前缀：pms:cache:{name}:{key} */
    public static final String CACHE_PREFIX = "pms:cache:";

    private RedisKeys() {
    }

    public static String refresh(String jti) {
        return REFRESH_PREFIX + jti;
    }

    public static String blacklist(String jti) {
        return BLACKLIST_PREFIX + jti;
    }

    public static String userSessions(String uid) {
        return USER_SESSIONS_PREFIX + uid;
    }

    public static String loginFail(String username) {
        return LOGIN_FAIL_PREFIX + username;
    }

    public static String cache(String name, String key) {
        return CACHE_PREFIX + name + ":" + key;
    }
}
