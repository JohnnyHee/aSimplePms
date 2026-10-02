package com.jyh.pms.storage;

import com.jyh.pms.core.web.PageQuery;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 通用文档仓储抽象。让业务代码完全不关心底层是 MongoDB 还是本地内存实现：
 * <ul>
 *   <li>{@code mongoQuery} —— MongoDB 实现使用的原生 Query（内存实现忽略）</li>
 *   <li>{@code filter} —— 内存实现使用的断言（MongoDB 实现忽略）</li>
 *   <li>{@code sort} —— 统一排序选项</li>
 * </ul>
 * 两个实现类通过 {@code pms.storage} 配置互斥生效。
 *
 * @param <T> 实体类型
 */
public interface DocumentRepository<T> {

    T save(T entity);

    List<T> saveAll(List<T> entities);

    Optional<T> findById(String id);

    List<T> findAll();

    List<T> findAllById(List<String> ids);

    boolean existsById(String id);

    long count();

    /** 条件计数。 */
    long count(Query mongoQuery, Predicate<T> filter);

    /** 分页查询。 */
    PageData<T> page(Query mongoQuery, Predicate<T> filter, PageQuery pageQuery, SortOption... sorts);

    /** 条件查询（不分页）。 */
    List<T> find(Query mongoQuery, Predicate<T> filter, SortOption... sorts);

    /** 条件查询单条。 */
    Optional<T> findOne(Query mongoQuery, Predicate<T> filter);

    boolean deleteById(String id);

    long deleteAllById(List<String> ids);

    /** 删除实体类的全部数据（审计日志清理等场景）。 */
    long deleteAll(Query mongoQuery, Predicate<T> filter);

    Class<T> entityType();
}
