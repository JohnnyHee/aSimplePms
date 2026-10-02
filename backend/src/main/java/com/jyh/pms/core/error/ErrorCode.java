package com.jyh.pms.core.error;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。约定：
 * <ul>
 *   <li>{@code 0} 成功</li>
 *   <li>{@code 1xxx} 参数/校验错误（HTTP 400）</li>
 *   <li>{@code 2xxx} 认证错误（HTTP 401）</li>
 *   <li>{@code 3xxx} 权限错误（HTTP 403）</li>
 *   <li>{@code 4xxx} 资源/业务冲突（HTTP 404 / 409 / 422）</li>
 *   <li>{@code 9xxx} 系统错误（HTTP 500）</li>
 * </ul>
 * <p>2003 账号锁定用 HTTP 423，避免前端把「临时锁定」误判为「会话失效」。</p>
 */
public enum ErrorCode {

    SUCCESS(0, "成功", HttpStatus.OK),

    BAD_REQUEST(1000, "请求参数不合法", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(1001, "参数校验失败", HttpStatus.BAD_REQUEST),
    NOT_FOUND(4000, "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(4001, "资源已存在或状态冲突", HttpStatus.CONFLICT),
    BUSINESS_ERROR(4002, "业务处理失败", HttpStatus.UNPROCESSABLE_ENTITY),

    UNAUTHENTICATED(2000, "未登录或登录状态已失效", HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS(2001, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(2002, "账号已被禁用", HttpStatus.UNAUTHORIZED),
    /* 423 而不是 401：前端对 401 会直接清理会话跳登录页，而锁定只是临时状态 */
    ACCOUNT_LOCKED(2003, "账号已被锁定，请稍后再试", HttpStatus.LOCKED),
    TOKEN_EXPIRED(2004, "登录已过期，请重新登录", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2005, "令牌无效", HttpStatus.UNAUTHORIZED),

    ACCESS_DENIED(3000, "没有访问该资源的权限", HttpStatus.FORBIDDEN),

    INTERNAL_ERROR(9000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
