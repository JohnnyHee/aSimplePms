package com.jyh.pms.storage.mongo;

import com.jyh.pms.domain.AuditLog;
import com.jyh.pms.storage.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 审计日志仓储的 MongoDB 实现（{@code pms.storage=mongodb} 生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "mongodb")
public class MongoAuditLogRepository extends AbstractMongoDomainRepository<AuditLog> implements AuditLogRepository {

    public MongoAuditLogRepository(MongoTemplate mongoTemplate) {
        super(mongoTemplate, AuditLog.class);
    }

    @Override
    public long deleteCreatedBefore(Instant time) {
        if (time == null) {
            return 0L;
        }
        return deleteAll(new Query(Criteria.where("createdAt").lt(time)), null);
    }
}
