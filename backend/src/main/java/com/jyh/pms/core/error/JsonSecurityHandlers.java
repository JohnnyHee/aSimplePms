package com.jyh.pms.core.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jyh.pms.core.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Spring Security 的 401/403 出口：直接写统一 JSON，不重定向到登录页，保持前后端分离语义。
 */
public final class JsonSecurityHandlers {

    private JsonSecurityHandlers() {
    }

    /** 未认证（缺少或非法令牌）。 */
    public static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> write(objectMapper, response,
                ErrorCode.UNAUTHENTICATED, resolveMessage(authException));
    }

    /** 已认证但权限不足。 */
    public static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, deniedException) -> write(objectMapper, response,
                ErrorCode.ACCESS_DENIED, null);
    }

    private static String resolveMessage(AuthenticationException exception) {
        if (exception == null || exception.getMessage() == null) {
            return null;
        }
        return "登录状态已失效，请重新登录";
    }

    private static void write(ObjectMapper objectMapper, HttpServletResponse response, ErrorCode errorCode,
                              String message) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(errorCode, message == null ? errorCode.message() : message));
    }

    /** 兼容旧调用：仅用于日志的请求描述。 */
    public static String describe(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }
}
