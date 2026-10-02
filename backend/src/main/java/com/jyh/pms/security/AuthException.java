package com.jyh.pms.security;

/**
 * 认证流程中抛出的异常，由 {@link com.jyh.pms.security.JwtAuthenticationEntryPoint} 转换为统一 JSON 响应。
 */
public class AuthException extends RuntimeException {

    private final com.jyh.pms.core.error.ErrorCode errorCode;

    public AuthException(com.jyh.pms.core.error.ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public com.jyh.pms.core.error.ErrorCode getErrorCode() {
        return errorCode;
    }

    public static AuthException unauthenticated(String message) {
        return new AuthException(com.jyh.pms.core.error.ErrorCode.UNAUTHENTICATED, message);
    }

    public static AuthException accessDenied(String message) {
        return new AuthException(com.jyh.pms.core.error.ErrorCode.ACCESS_DENIED, message);
    }
}
