package com.jyh.pms.security;

import com.jyh.pms.core.config.PmsProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 签发与解析。访问令牌为无状态 JWT，刷新令牌为 JWT + 服务端会话（存 Redis / 内存），
 * 从而既能水平扩展，又支持主动失效。
 */
@Component
public class JwtTokenProvider {

    private final PmsProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(PmsProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("pms.security.jwt-secret 至少需要 32 字节，当前 " + keyBytes.length + " 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** 访问令牌有效期。 */
    public Duration accessTtl() {
        return properties.getSecurity().getAccessTokenTtl();
    }

    /** 刷新令牌有效期。 */
    public Duration refreshTtl() {
        return properties.getSecurity().getRefreshTokenTtl();
    }

    /**
     * 签发访问令牌。
     *
     * @param uid         用户 id
     * @param username    登录名
     * @param roles       角色编码
     * @param permissions 权限编码
     */
    public IssuedToken issueAccessToken(String uid, String username, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTtl());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(properties.getSecurity().getIssuer())
                .subject(uid)
                .id(jti)
                .claim("username", username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
        return new IssuedToken(token, jti, now, expiry);
    }

    /** 签发刷新令牌（jti 由调用方保存到令牌存储）。 */
    public IssuedToken issueRefreshToken(String uid, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTtl());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(properties.getSecurity().getIssuer())
                .subject(uid)
                .id(jti)
                .claim("username", username)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
        return new IssuedToken(token, jti, now, expiry);
    }

    /**
     * 解析并校验令牌。
     *
     * @throws AuthException 过期或非法
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getSecurity().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new AuthException(com.jyh.pms.core.error.ErrorCode.TOKEN_EXPIRED, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(com.jyh.pms.core.error.ErrorCode.TOKEN_INVALID, "令牌无效");
        }
    }

    /** 解析但不校验过期（用于登出时把已过期的令牌也拉黑）。 */
    public Claims parseIgnoringExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> readStringList(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /** 令牌签发结果。 */
    public record IssuedToken(String token, String jti, Instant issuedAt, Instant expiresAt) {
    }
}
