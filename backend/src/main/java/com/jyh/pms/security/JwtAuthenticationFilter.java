package com.jyh.pms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jyh.pms.core.error.ErrorCode;
import com.jyh.pms.core.web.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * JWT 认证过滤器：从 {@code Authorization: Bearer <token>} 还原无状态登录态，
 * 校验签名、过期时间与黑名单；失败时直接返回统一 JSON 401，不重定向到登录页。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenStore tokenStore, ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = tokenProvider.parse(token);
            if (!"access".equals(claims.get("type", String.class))) {
                throw new AuthException(ErrorCode.TOKEN_INVALID, "令牌类型不正确");
            }
            if (tokenStore.isAccessTokenBlacklisted(claims.getId())) {
                throw new AuthException(ErrorCode.TOKEN_INVALID, "令牌已失效，请重新登录");
            }
            String uid = claims.getSubject();
            String username = claims.get("username", String.class);
            List<String> roles = tokenProvider.readStringList(claims, "roles");
            List<String> permissions = tokenProvider.readStringList(claims, "permissions");
            AuthUser principal = new AuthUser(uid, username, roles, permissions);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("pms.username", username);
            request.setAttribute("pms.uid", uid);
            chain.doFilter(request, response);
        } catch (AuthException e) {
            SecurityContextHolder.clearContext();
            writeError(response, e.getErrorCode(), e.getMessage());
        }
    }

    /** 解析请求头中的 Bearer 令牌。 */
    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    /** 令牌剩余有效期，用于登出时把访问令牌加入黑名单。 */
    public Duration remainingTtl(Claims claims) {
        Instant expiration = claims.getExpiration().toInstant();
        Duration remaining = Duration.between(Instant.now(), expiration);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(errorCode, message == null ? errorCode.message() : message));
    }
}
