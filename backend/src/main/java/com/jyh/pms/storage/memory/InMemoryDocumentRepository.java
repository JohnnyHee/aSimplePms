package com.jyh.pms.storage.memory;

import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.storage.DocumentRepository;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.SortOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 内存版通用仓储：本地开发零依赖运行（无需安装 MongoDB）。
 *
 * <p>数据保存在进程内存中，重启即清空，因此仅用于开发/演示，生产请使用 {@code pms.storage=mongodb}。
 *
 * <p><b>类型隔离：</b>每个实体仓储（人员/角色/职位/薪资/审计日志）各自持有一个本类实例，
 * 并通过 {@link #forType(Class)} 绑定自己的实体类型；所有读写都只作用于该类型的内部 Map，
 * 绝不会跨实体类型返回数据。各实例共享同一份进程内数据（静态 {@code STORES}），
 * 因此不同仓储之间仍能看到彼此写入的关联数据。
 */
@Component
@ConditionalOnProperty(name = "pms.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryDocumentRepository implements DocumentRepository<Object> {

    /** 进程内共享的数据池：实体类型 → (id → 实体)。 */
    private static final Map<Class<?>, Map<String, Object>> STORES = new ConcurrentHashMap<>();

    /** 由各实体仓储通过 {@link #forType(Class)} 注入的类型化视图。 */
    private Class<?> type = Object.class;

    /**
     * 返回绑定到指定实体类型的视图。
     * 各实体仓储在内存模式下用它与本 Bean 共享同一份数据。
     */
    public InMemoryDocumentRepository forType(Class<?> entityType) {
        InMemoryDocumentRepository scoped = new InMemoryDocumentRepository();
        scoped.type = entityType;
        return scoped;
    }

    @Override
    public Object save(Object entity) {
        String id = IdSupport.ensureId(entity);
        storeOf(entity.getClass()).put(id, entity);
        return entity;
    }

    @Override
    public List<Object> saveAll(List<Object> entities) {
        entities.forEach(this::save);
        return entities;
    }

    @Override
    public Optional<Object> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storeOf(type).get(id));
    }

    @Override
    public List<Object> findAll() {
        return new ArrayList<>(storeOf(type).values());
    }

    @Override
    public List<Object> findAllById(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<String, Object> store = storeOf(type);
        List<Object> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Object found = store.get(id);
            if (found != null) {
                result.add(found);
            }
        }
        return result;
    }

    @Override
    public boolean existsById(String id) {
        return id != null && storeOf(type).containsKey(id);
    }

    @Override
    public long count() {
        return storeOf(type).size();
    }

    @Override
    public long count(Query mongoQuery, Predicate<Object> filter) {
        return findAll().stream().filter(filter).count();
    }

    @Override
    public PageData<Object> page(Query mongoQuery, Predicate<Object> filter, PageQuery pageQuery, SortOption... sorts) {
        List<Object> matched = new ArrayList<>(findAll().stream().filter(filter).toList());
        matched.sort(comparator(sorts));
        long total = matched.size();
        int from = Math.min(pageQuery.offset(), matched.size());
        int to = Math.min(from + pageQuery.getSize(), matched.size());
        return new PageData<>(new ArrayList<>(matched.subList(from, to)), total);
    }

    @Override
    public List<Object> find(Query mongoQuery, Predicate<Object> filter, SortOption... sorts) {
        List<Object> matched = new ArrayList<>(findAll().stream().filter(filter).toList());
        matched.sort(comparator(sorts));
        return matched;
    }

    @Override
    public Optional<Object> findOne(Query mongoQuery, Predicate<Object> filter) {
        return findAll().stream().filter(filter).findFirst();
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return storeOf(type).remove(id) != null;
    }

    @Override
    public long deleteAllById(List<String> ids) {
        if (ids == null) {
            return 0;
        }
        return ids.stream().filter(this::deleteById).count();
    }

    @Override
    public long deleteAll(Query mongoQuery, Predicate<Object> filter) {
        List<Object> matched = findAll().stream().filter(filter).toList();
        matched.forEach(entity -> deleteById(IdSupport.idOf(entity)));
        return matched.size();
    }

    @Override
    public Class<Object> entityType() {
        return Object.class;
    }

    /** 清空所有数据（仅测试/演示使用）。 */
    public void clear() {
        STORES.clear();
    }

    private Map<String, Object> storeOf(Class<?> type) {
        return STORES.computeIfAbsent(type, key -> new ConcurrentHashMap<>());
    }

    private Comparator<Object> comparator(SortOption... sorts) {
        if (sorts == null || sorts.length == 0) {
            return (a, b) -> 0;
        }
        Comparator<Object> comparator = null;
        for (SortOption sort : sorts) {
            Comparator<Object> next = Comparator.comparing(
                    entity -> (Comparable<Object>) comparableValue(IdSupport.readField(entity, sort.field())),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            if (sort.desc()) {
                next = next.reversed();
            }
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        return comparator == null ? (a, b) -> 0 : comparator;
    }

    @SuppressWarnings("unchecked")
    private Comparable<Object> comparableValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Comparable<?> comparable) {
            return (Comparable<Object>) comparable;
        }
        return (Comparable<Object>) (Comparable<?>) String.valueOf(value);
    }

    /** 反射工具：读取 {@code @Id} 字段与排序字段。 */
    static final class IdSupport {

        private static final Map<Class<?>, Field> ID_FIELDS = new ConcurrentHashMap<>();
        private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

        private IdSupport() {
        }

        static Field idField(Class<?> type) {
            return ID_FIELDS.computeIfAbsent(type, key -> {
                for (Field field : allFields(key)) {
                    if (field.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                        field.setAccessible(true);
                        return field;
                    }
                }
                throw new IllegalStateException("实体缺少 @Id 字段: " + key.getName());
            });
        }

        static String idOf(Object entity) {
            Object value = read(idField(entity.getClass()), entity);
            return value == null ? null : String.valueOf(value);
        }

        static String ensureId(Object entity) {
            Field field = idField(entity.getClass());
            Object value = read(field, entity);
            if (value == null || String.valueOf(value).isBlank()) {
                String generated = java.util.UUID.randomUUID().toString();
                write(field, entity, generated);
                return generated;
            }
            return String.valueOf(value);
        }

        static Object readField(Object entity, String name) {
            if (entity == null || name == null) {
                return null;
            }
            Field field = FIELD_CACHE
                    .computeIfAbsent(entity.getClass(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(name, key -> {
                        for (Field candidate : allFields(entity.getClass())) {
                            if (candidate.getName().equals(key)) {
                                candidate.setAccessible(true);
                                return candidate;
                            }
                        }
                        return null;
                    });
            return field == null ? null : read(field, entity);
        }

        static List<Field> allFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            Class<?> current = type;
            while (current != null && current != Object.class) {
                fields.addAll(List.of(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            return fields;
        }

        static Object read(Field field, Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + field.getName(), e);
            }
        }

        static void write(Field field, Object target, Object value) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("写入字段失败: " + field.getName(), e);
            }
        }

        /** 比较辅助：把常见可比较类型统一化。 */
        static Object normalize(Object value) {
            if (value instanceof Instant instant) {
                return instant;
            }
            if (value instanceof LocalDate date) {
                return date;
            }
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            return value;
        }
    }
}
