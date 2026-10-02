package com.jyh.pms.storage.mongo;

import com.jyh.pms.domain.Position;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.SortOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 职位仓储的 MongoDB 实现（{@code pms.storage=mongodb} 生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "mongodb")
public class MongoPositionRepository extends AbstractMongoDomainRepository<Position> implements PositionRepository {

    public MongoPositionRepository(MongoTemplate mongoTemplate) {
        super(mongoTemplate, Position.class);
    }

    @Override
    public Optional<Position> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return findOne(new Query(Criteria.where("code").is(code)), null);
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null) {
            return false;
        }
        return mongo().exists(new Query(Criteria.where("code").is(code)), Position.class);
    }

    @Override
    public List<Position> findAllOrderBySortOrder() {
        return find(null, null, SortOption.asc("sortOrder"));
    }
}
