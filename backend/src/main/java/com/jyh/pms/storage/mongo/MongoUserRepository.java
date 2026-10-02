package com.jyh.pms.storage.mongo;

import com.jyh.pms.domain.User;
import com.jyh.pms.storage.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 人员仓储的 MongoDB 实现（{@code pms.storage=mongodb} 生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "mongodb")
public class MongoUserRepository extends AbstractMongoDomainRepository<User> implements UserRepository {

    public MongoUserRepository(MongoTemplate mongoTemplate) {
        super(mongoTemplate, User.class);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return findOne(new Query(Criteria.where("username").is(username)), null);
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null) {
            return false;
        }
        return mongo().exists(new Query(Criteria.where("username").is(username)), User.class);
    }

    @Override
    public List<User> findByRoleId(String roleId) {
        if (roleId == null) {
            return List.of();
        }
        return find(new Query(Criteria.where("roleIds").is(roleId)), null);
    }

    @Override
    public long countByPositionId(String positionId) {
        if (positionId == null) {
            return 0L;
        }
        return mongo().count(new Query(Criteria.where("positionId").is(positionId)), User.class);
    }

    /**
     * 忽略大小写的精确匹配（供登录等场景直接使用）。
     * 注意：$regex 无法命中 username 的唯一索引，仅作兜底查询。
     */
    public Optional<User> findByUsernameIgnoreCase(String username) {
        if (username == null) {
            return Optional.empty();
        }
        Criteria criteria = Criteria.where("username").regex("^" + Pattern.quote(username) + "$", "i");
        return findOne(new Query(criteria), null);
    }
}
