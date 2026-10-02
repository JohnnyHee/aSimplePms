package com.jyh.pms.storage.mongo;

import com.jyh.pms.domain.Role;
import com.jyh.pms.storage.RoleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓储的 MongoDB 实现（{@code pms.storage=mongodb} 生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "mongodb")
public class MongoRoleRepository extends AbstractMongoDomainRepository<Role> implements RoleRepository {

    public MongoRoleRepository(MongoTemplate mongoTemplate) {
        super(mongoTemplate, Role.class);
    }

    @Override
    public Optional<Role> findByCode(String code) {
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
        return mongo().exists(new Query(Criteria.where("code").is(code)), Role.class);
    }

    @Override
    public List<Role> findByCodeIn(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return find(new Query(Criteria.where("code").in(codes)), null);
    }
}
