package com.jyh.pms.storage.mongo;

import com.jyh.pms.domain.Salary;
import com.jyh.pms.domain.SalaryStatus;
import com.jyh.pms.storage.SalaryRepository;
import com.jyh.pms.storage.SortOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 薪资仓储的 MongoDB 实现（{@code pms.storage=mongodb} 生效）。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "mongodb")
public class MongoSalaryRepository extends AbstractMongoDomainRepository<Salary> implements SalaryRepository {

    public MongoSalaryRepository(MongoTemplate mongoTemplate) {
        super(mongoTemplate, Salary.class);
    }

    @Override
    public List<Salary> findByUidOrderByEffectiveFromDesc(String uid) {
        if (uid == null) {
            return List.of();
        }
        return find(new Query(Criteria.where("uid").is(uid)), null, SortOption.desc("effectiveFrom"));
    }

    @Override
    public Optional<Salary> findActiveByUid(String uid) {
        if (uid == null) {
            return Optional.empty();
        }
        // 生效中 = status ACTIVE 且 effectiveTo 为空（未截止）；effectiveTo 为空既可能是 null 也可能是字段缺失
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("uid").is(uid),
                Criteria.where("status").is(SalaryStatus.ACTIVE),
                new Criteria().orOperator(
                        Criteria.where("effectiveTo").is(null),
                        Criteria.where("effectiveTo").exists(false)));
        return findOne(new Query(criteria).with(sort(SortOption.desc("effectiveFrom"))), null);
    }

    @Override
    public long countByUid(String uid) {
        if (uid == null) {
            return 0L;
        }
        return mongo().count(new Query(Criteria.where("uid").is(uid)), Salary.class);
    }

    @Override
    public long countByPositionId(String positionId) {
        if (positionId == null) {
            return 0L;
        }
        return mongo().count(new Query(Criteria.where("positionId").is(positionId)), Salary.class);
    }

    /**
     * 查询某人在指定日期生效的薪资（用于薪资试算）。
     * 内存实现未提供同名方法，Service 层请只依赖接口中声明的方法。
     */
    public Optional<Salary> findActiveByUidOn(String uid, LocalDate date) {
        if (uid == null || date == null) {
            return Optional.empty();
        }
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("uid").is(uid),
                Criteria.where("status").is(SalaryStatus.ACTIVE),
                new Criteria().orOperator(
                        Criteria.where("effectiveFrom").is(null),
                        Criteria.where("effectiveFrom").lte(date)),
                new Criteria().orOperator(
                        Criteria.where("effectiveTo").is(null),
                        Criteria.where("effectiveTo").exists(false),
                        Criteria.where("effectiveTo").gte(date)));
        return findOne(new Query(criteria).with(sort(SortOption.desc("effectiveFrom"))), null);
    }
}
