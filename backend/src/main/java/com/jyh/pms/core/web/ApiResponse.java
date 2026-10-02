package com.jyh.pms.core.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jyh.pms.core.error.ErrorCode;

/**
 * 统一的 JSON 响应信封：{@code {"code":0,"message":"成功","data":{...},"timestamp":"..."}}。
 * 前后台之间只传输该结构的 JSON 字符串。
 *
 * @param code      业务码，0 表示成功
 * @param message   提示信息
 * @param data      业务数据，失败时为 null
 * @param timestamp 服务器时间（ISO-8601）
 */
public record ApiResponse<T>(int code, String message, T data, String timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.message());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null, now());
    }

    /**
     * 便捷判断，不参与 JSON 序列化：否则 Jackson 会把 record 上的 {@code isSuccess()}
     * 当成 bean 属性，序列化出契约外的 {@code "success":true} 字段。
     */
    @JsonIgnore
    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.code();
    }

    private static String now() {
        return java.time.OffsetDateTime.now().toString();
    }
}
