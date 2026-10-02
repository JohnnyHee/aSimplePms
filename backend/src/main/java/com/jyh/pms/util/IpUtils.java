package com.jyh.pms.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 客户端 IP 解析工具，兼容常见反向代理头。
 */
public final class IpUtils {

    private static final List<String> HEADERS = List.of(
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP");

    private IpUtils() {
    }

    /** 获取客户端 IP，取不到时返回 {@code unknown}。 */
    public static String clientIp(HttpServletRequest request) {
        for (String header : HEADERS) {
            String value = request.getHeader(header);
            if (isValid(value)) {
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private static boolean isValid(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }
}
