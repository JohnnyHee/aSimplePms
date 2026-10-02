package com.jyh.pms.storage.mongo;

import com.jyh.pms.domain.AuditLog;
import com.jyh.pms.domain.Position;
import com.jyh.pms.domain.Role;
import com.jyh.pms.domain.Salary;
import com.jyh.pms.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * MongoDB 索引初始化：应用启动时为各集合补齐唯一索引与查询索引。
 *
 * <p>只在 {@code pms.storage=mongodb} 时生效。判断「是否已存在」时比较的是
 * <b>索引语义</b>（字段顺序 + 是否唯一 + 是否稀疏）而不是索引名——实体上的
 * {@code @Indexed} / {@code @CompoundIndex} 会被 Spring Data 以 {@code code_1}
 * 这类默认名建出来，与这里指定的 {@code roles.code} 语义等价但名字不同；
 * 若按名字判断就会去重复创建，然后被 MongoDB 以
 * {@code IndexOptionsConflict} 拒绝并打出误导性的告警。</p>
 *
 * <p>索引创建失败（例如历史数据存在重复键）只记录日志，绝不让应用启动失败。</p>
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "mongodb")
public class MongoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化 MongoDB 索引 ...");
        try {
            ensureIndex(User.class, "users.username", new Index().on("username", Sort.Direction.ASC).unique());
            ensureIndex(Role.class, "roles.code", new Index().on("code", Sort.Direction.ASC).unique());
            ensureIndex(Position.class, "positions.code", new Index().on("code", Sort.Direction.ASC).unique());
            ensureIndex(Salary.class, "salaries.uid", new Index().on("uid", Sort.Direction.ASC));
            ensureIndex(AuditLog.class, "audit_logs.createdAt", new Index().on("createdAt", Sort.Direction.DESC));
            log.info("MongoDB 索引初始化完成");
        } catch (Exception e) {
            log.warn("MongoDB 索引初始化过程出现异常，已忽略（不影响启动）: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建单个索引；已存在语义等价的索引时直接跳过。
     *
     * @param type        实体类型（决定集合名）
     * @param description 便于阅读的索引描述，如 {@code users.username}
     * @param index       索引定义
     */
    private void ensureIndex(Class<?> type, String description, Index index) {
        try {
            IndexOperations operations = mongoTemplate.indexOps(type);
            String existing = findEquivalent(operations, index);
            if (existing != null) {
                log.info("索引已存在，跳过: {}（现有索引名 {}）", description, existing);
                return;
            }
            String created = operations.createIndex(index);
            log.info("索引已创建: {} -> {}", description, created);
        } catch (Exception e) {
            log.warn("索引创建失败（已忽略）: {} -> {}", description, e.getMessage());
        }
    }

    /**
     * 查找与目标索引语义等价的现有索引。
     *
     * <p>等价条件是：索引键完全一致（字段名与方向、顺序都相同）、唯一性一致、
     * 稀疏性一致。索引名不参与比较。</p>
     *
     * <p>注意两侧的元素类型并不相同：{@link Index#getIndexKeys()} 给的是
     * {@code org.bson.Document}（元素是 {@code Map.Entry}），而
     * {@link IndexInfo#getIndexFields()} 给的是 {@code IndexField}。所以先把两边都
     * 归一化成 {@code 字段名:方向} 文本列表再比较，直接按元素比较会永远不相等。</p>
     *
     * @return 现有索引名；没有等价索引时返回 {@code null}
     */
    private String findEquivalent(IndexOperations operations, Index target) {
        List<IndexInfo> existing;
        try {
            existing = operations.getIndexInfo();
        } catch (Exception e) {
            log.warn("读取索引信息失败（将继续尝试创建）: {}", e.getMessage());
            return null;
        }

        List<String> targetKeys = target.getIndexKeys().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + direction(entry.getValue()))
                .toList();
        boolean targetUnique = Boolean.TRUE.equals(target.getIndexOptions().get("unique"));
        boolean targetSparse = Boolean.TRUE.equals(target.getIndexOptions().get("sparse"));

        for (IndexInfo info : existing) {
            List<String> infoKeys = info.getIndexFields().stream()
                    .map(field -> field.getKey() + ":" + field.getDirection().name())
                    .toList();
            if (!infoKeys.equals(targetKeys)) {
                continue;
            }
            if (info.isUnique() != targetUnique || info.isSparse() != targetSparse) {
                continue;
            }
            return info.getName();
        }
        return null;
    }

    /** 把方向值（{@code 1}/{@code -1}/{@code "text"} 或 {@code Sort.Direction}）归一化成文本。 */
    private static String direction(Object value) {
        if (value instanceof Number number) {
            return number.intValue() >= 0 ? "ASC" : "DESC";
        }
        if (value instanceof Sort.Direction dir) {
            return dir.name();
        }
        if (value == null) {
            return "ASC";
        }
        String text = String.valueOf(value).toUpperCase(Locale.ROOT);
        if ("1".equals(text)) {
            return "ASC";
        }
        if ("-1".equals(text)) {
            return "DESC";
        }
        return text;
    }
}
