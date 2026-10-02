package com.jyh.pms.core.error;

import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.security.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：所有异常统一转换为 {@link ApiResponse} JSON 结构。
 *
 * <p>HTTP 状态码取自 {@link ErrorCode#status()}，业务码放在 body 的 {@code code} 字段，
 * 两者一一对应（如 2001 用户名或密码错误 &rarr; HTTP 401，3000 无权限 &rarr; HTTP 403，
 * 4002 业务处理失败 &rarr; HTTP 422）。前端既可按 HTTP 状态分流，也可只读 body 的 code。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(e.getErrorCode(), e.getMessage());
    }

    /** 认证异常。 */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthException e, HttpServletRequest request) {
        log.warn("认证异常 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(e.getErrorCode(), e.getMessage());
    }

    /** @Valid 校验失败。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .distinct()
                .collect(Collectors.joining("；"));
        if (message.isBlank()) {
            message = ErrorCode.VALIDATION_FAILED.message();
        }
        return build(ErrorCode.VALIDATION_FAILED, message);
    }

    /** 方法参数约束校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return build(ErrorCode.VALIDATION_FAILED,
                message.isBlank() ? ErrorCode.VALIDATION_FAILED.message() : message);
    }

    /** 请求体缺参数或格式错误。 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("请求参数错误 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(ErrorCode.BAD_REQUEST, "请求参数格式不正确");
    }

    /** 无访问权限。 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("拒绝访问 [{} {}]", request.getMethod(), request.getRequestURI());
        return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.message());
    }

    /** 请求方法不支持。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return build(ErrorCode.BAD_REQUEST, "不支持的请求方法: " + e.getMethod());
    }

    /** 静态资源/接口路径不存在。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return build(ErrorCode.NOT_FOUND, "接口不存在: " + e.getResourcePath());
    }

    /** 唯一索引冲突（绕过业务校验时的兜底）。 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return build(ErrorCode.CONFLICT, "数据已存在，请勿重复提交");
    }

    /** 兜底异常。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message) {
        HttpStatus status = errorCode.status() == null ? HttpStatus.INTERNAL_SERVER_ERROR : errorCode.status();
        return ResponseEntity.status(status).body(ApiResponse.error(errorCode, message));
    }

    private String describe(FieldError error) {
        String message = error.getDefaultMessage();
        return error.getField() + (message == null ? " 不合法" : ": " + message);
    }
}
