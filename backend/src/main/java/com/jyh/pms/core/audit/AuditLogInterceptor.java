package com.jyh.pms.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.domain.AuditLog;
import com.jyh.pms.security.AuthException;
import com.jyh.pms.security.JwtAuthenticationFilter;
import com.jyh.pms.storage.AuditLogRepository;
import com.jyh.pms.util.IpUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

/**
 * 审计拦截器：对标注 {@link Audited} 的接口采集动作、操作人、来源 IP、耗时与结果，
 * 落库到 {@code audit_logs}。审计写入失败只记录警告，绝不影响业务请求。
 */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditLogInterceptor.class);
    private static final String START_ATTRIBUTE = "pms.audit.start";
    private static final int MAX_DETAIL_LENGTH = 500;

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final com.jyh.pms.security.JwtTokenProvider tokenProvider;

    public AuditLogInterceptor(AuditLogRepository auditLogRepository, ObjectMapper objectMapper,
                               com.jyh.pms.security.JwtTokenProvider tokenProvider) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod && handlerMethod.hasMethodAnnotation(Audited.class)) {
            request.setAttribute(START_ATTRIBUTE, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        Audited audited = handlerMethod.getMethodAnnotation(Audited.class);
        if (audited == null) {
            return;
        }
        try {
            Object start = request.getAttribute(START_ATTRIBUTE);
            long elapsed = start instanceof Long value ? System.currentTimeMillis() - value : 0L;
            AuditLog entity = new AuditLog();
            entity.setUsername(resolveUsername(request));
            entity.setUid((String) request.getAttribute("pms.uid"));
            entity.setAction(audited.action());
            entity.setMethod(request.getMethod());
            entity.setPath(request.getRequestURI());
            entity.setIp(IpUtils.clientIp(request));
            entity.setOutcome(resolveOutcome(response, ex));
            entity.setDetail(audited.recordDetail() ? truncate(detail(request)) : null);
            entity.setElapsedMs(elapsed);
            entity.setCreatedAt(Instant.now());
            auditLogRepository.save(entity);
        } catch (Exception e) {
            log.warn("写入审计日志失败: {}", e.getMessage());
        }
    }

    private String resolveUsername(HttpServletRequest request) {
        String explicit = AuditExpressions.explicitUsername(request);
        if (explicit != null) {
            return explicit;
        }
        String username = SecurityUtils.currentUsername();
        if (username != null) {
            return username;
        }
        username = (String) request.getAttribute("pms.username");
        if (username != null) {
            return username;
        }
        String token = JwtAuthenticationFilter.resolveToken(request);
        if (token == null) {
            return "anonymous";
        }
        try {
            Claims claims = tokenProvider.parseIgnoringExpiration(token);
            String claimUsername = claims.get("username", String.class);
            return claimUsername == null ? "anonymous" : claimUsername;
        } catch (AuthException e) {
            return "anonymous";
        }
    }

    private String resolveOutcome(HttpServletResponse response, @Nullable Exception ex) {
        if (ex != null) {
            return "FAILURE";
        }
        return response.getStatus() >= 400 ? "FAILURE" : "SUCCESS";
    }

    private String detail(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return null;
        }
        return "?" + query;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL_LENGTH ? value : value.substring(0, MAX_DETAIL_LENGTH);
    }
}
