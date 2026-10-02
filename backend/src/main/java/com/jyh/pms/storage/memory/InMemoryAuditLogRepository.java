package com.jyh.pms.storage.memory;

import com.jyh.pms.domain.AuditLog;
import com.jyh.pms.storage.AuditLogRepository;
import com.jyh.pms.storage.DocumentRepository;
import com.jyh.pms.storage.support.AbstractDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 审计日志仓储的内存实现（{@code pms.storage=memory}，缺省生效）。
 *
 * <p>按时间清理走「谓词过滤 + deleteAll」路径，由底层内存仓储负责逐条删除。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuditLogRepository extends AbstractDocumentRepository<AuditLog> implements AuditLogRepository {

    public InMemoryAuditLogRepository(InMemoryDocumentRepository store) {
        super(store.forType(AuditLog.class), AuditLog.class);
    }

    @Override
    public long deleteCreatedBefore(Instant time) {
        if (time == null) {
            return 0L;
        }
        return deleteAll(null, log -> log.getCreatedAt() != null && log.getCreatedAt().isBefore(time));
    }
}
