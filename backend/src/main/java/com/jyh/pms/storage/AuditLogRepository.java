package com.jyh.pms.storage;

import com.jyh.pms.domain.AuditLog;

import java.time.Instant;

/**
 * 审计日志（AuditLog）查询仓储。通用 CRUD 由 {@link DocumentRepository} 提供，
 * 这里额外提供按时间批量清理的能力。
 */
public interface AuditLogRepository extends DocumentRepository<AuditLog> {

    /**
     * 删除创建时间早于给定时刻的日志（保留期清理）。
     *
     * @param time 时间界限（不含）
     * @return 实际删除条数
     */
    long deleteCreatedBefore(Instant time);
}
