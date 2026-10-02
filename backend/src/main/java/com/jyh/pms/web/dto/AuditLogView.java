package com.jyh.pms.web.dto;

import java.time.Instant;

/**
 * 审计日志对外视图。
 */
public record AuditLogView(
        String id,
        String username,
        String uid,
        String action,
        String method,
        String path,
        String ip,
        String outcome,
        String detail,
        long elapsedMs,
        Instant createdAt) {
}
