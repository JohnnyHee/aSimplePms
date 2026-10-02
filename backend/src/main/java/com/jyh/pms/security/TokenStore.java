package com.jyh.pms.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 令牌存储抽象：Redis 版（生产）与内存版（本地开发）互斥生效，通过 {@code pms.token-store=<redis|memory>} 选择。
 * Redis 键位约定见 {@link RedisKeys}。
 */
public interface TokenStore {

    /** 保存刷新令牌会话。 */
    void saveRefreshToken(RefreshSession session, Duration ttl);

    /** 读取刷新令牌会话。 */
    Optional<RefreshSession> findRefreshToken(String jti);

    /** 吊销（删除）刷新令牌。 */
    void revokeRefreshToken(String jti);

    /** 把访问令牌加入黑名单，直到其自然过期。 */
    void blacklistAccessToken(String jti, Duration ttl);

    /** 访问令牌是否已被拉黑。 */
    boolean isAccessTokenBlacklisted(String jti);

    /** 记录登录失败次数，返回累计值。 */
    long recordLoginFailure(String username, Duration ttl);

    /** 读取登录失败次数。 */
    long loginFailureCount(String username);

    /** 清除登录失败计数（登录成功时调用）。 */
    void clearLoginFailures(String username);

    /** 强制某用户全部会话下线（改密、禁用、删除时调用），返回吊销的令牌数。 */
    int revokeAllForUser(String uid);

    /** 当前用户在线会话数。 */
    int activeSessions(String uid);

    /** 清空所有数据（仅测试用）。 */
    void clear();

    /** 实际生效的实现名，用于启动日志。 */
    String implementation();
}
