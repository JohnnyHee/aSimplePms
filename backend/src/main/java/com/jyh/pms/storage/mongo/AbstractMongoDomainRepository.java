package com.jyh.pms.storage.mongo;

import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.storage.DocumentRepository;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.SortOption;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * MongoDB 领域仓储的公共基类：直接基于 {@link MongoTemplate} 实现
 * {@link DocumentRepository} 的通用 CRUD，具体仓储只需补充领域查询方法。
 *
 * <p><b>为什么不复用 {@link MongoDocumentRepository}？</b>
 * 该类实现的是 {@code DocumentRepository<User>}（泛型被写死为 User），
 * 在 {@code pms.storage=mongodb} 下它是容器里唯一的 {@code DocumentRepository} Bean，
 * 于是一个要求 {@code DocumentRepository<Role>} 的注入点会因 Spring 泛型匹配失败而无法装配。
 * 这里改为直接依赖 {@link MongoTemplate}（本身与实体类型无关），既能正确按实体类型读写，
 * 也彻底规避该泛型冲突；所有查询都显式传入 {@code entityType}。
 *
 * @param <T> 实体类型
 */
public abstract class AbstractMongoDomainRepository<T> implements DocumentRepository<T> {

    private final MongoTemplate mongoTemplate;
    private final Class<T> entityType;

    protected AbstractMongoDomainRepository(MongoTemplate mongoTemplate, Class<T> entityType) {
        this.mongoTemplate = mongoTemplate;
        this.entityType = entityType;
    }

    /** MongoDB 模板，供子类构造领域查询。 */
    protected MongoTemplate mongo() {
        return mongoTemplate;
    }

    @Override
    public T save(T entity) {
        return mongoTemplate.save(entity);
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(this::save).toList();
    }

    @Override
    public Optional<T> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mongoTemplate.findById(id, entityType));
    }

    @Override
    public List<T> findAll() {
        return mongoTemplate.findAll(entityType);
    }

    @Override
    public List<T> findAllById(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mongoTemplate.find(new Query(Criteria.where("_id").in(ids)), entityType);
    }

    @Override
    public boolean existsById(String id) {
        if (id == null) {
            return false;
        }
        return mongoTemplate.exists(new Query(Criteria.where("_id").is(id)), entityType);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), entityType);
    }

    @Override
    public long count(Query mongoQuery, Predicate<T> filter) {
        return mongoTemplate.count(normalize(mongoQuery), entityType);
    }

    @Override
    public PageData<T> page(Query mongoQuery, Predicate<T> filter, PageQuery pageQuery, SortOption... sorts) {
        Query query = normalize(mongoQuery);
        long total = mongoTemplate.count(query, entityType);
        query.with(sort(sorts)).skip(pageQuery.offset()).limit(pageQuery.getSize());
        return new PageData<>(mongoTemplate.find(query, entityType), total);
    }

    @Override
    public List<T> find(Query mongoQuery, Predicate<T> filter, SortOption... sorts) {
        return mongoTemplate.find(normalize(mongoQuery).with(sort(sorts)), entityType);
    }

    @Override
    public Optional<T> findOne(Query mongoQuery, Predicate<T> filter) {
        return Optional.ofNullable(mongoTemplate.findOne(normalize(mongoQuery), entityType));
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return mongoTemplate.remove(new Query(Criteria.where("_id").is(id)), entityType).getDeletedCount() > 0;
    }

    @Override
    public long deleteAllById(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return mongoTemplate.remove(new Query(Criteria.where("_id").in(ids)), entityType).getDeletedCount();
    }

    @Override
    public long deleteAll(Query mongoQuery, Predicate<T> filter) {
        return mongoTemplate.remove(normalize(mongoQuery), entityType).getDeletedCount();
    }

    @Override
    public Class<T> entityType() {
        return entityType;
    }

    /** 把排序选项转成 Spring Data 的 {@link Sort}。 */
    protected static Sort sort(SortOption... sorts) {
        if (sorts == null || sorts.length == 0) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = new ArrayList<>(sorts.length);
        for (SortOption option : sorts) {
            orders.add(new Sort.Order(option.desc() ? Sort.Direction.DESC : Sort.Direction.ASC, option.field()));
        }
        return Sort.by(orders);
    }

    /** 空 Query 兜底，避免 NPE 与「无条件删除全部」的意外发生。 */
    protected static Query normalize(Query query) {
        return query == null ? new Query() : query;
    }
}
