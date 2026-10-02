package com.jyh.pms.security;

import java.time.Instant;

/**
 * 登录后写入令牌存储的会话信息。支撑刷新令牌轮换、单点登出与"改密后强制下线"。
 *
 * @param jti        令牌唯一 id
 * @param uid        用户 id
 * @param username   登录名
 * @param issuedAt   签发时间
 * @param expiresAt  过期时间
 */
public record RefreshSession(String jti, String uid, String username, Instant issuedAt, Instant expiresAt) {
}
