package com.jyh.pms.storage.support;

import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.storage.DocumentRepository;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.SortOption;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 通用文档仓储的泛型适配基类。
 *
 * <p>底层 {@link DocumentRepository} 的实现类泛型参数各不相同
 * （内存实现是 {@code DocumentRepository<Object>}，MongoDB 实现是 {@code DocumentRepository<User>}），
 * 若在具体仓储中直接注入带泛型参数的 {@code DocumentRepository<T>}，
 * Spring 的泛型匹配会解析失败。因此这里统一：
 * <ul>
 *   <li>构造器只接受<b>原始类型</b> {@code DocumentRepository}，不做泛型校验；</li>
 *   <li>由本基类负责 {@code Object <-> T} 的强制类型转换，子类只关心业务谓词。</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public abstract class AbstractDocumentRepository<T> implements DocumentRepository<T> {

    /** 底层通用仓储（原始类型，避免 Spring 泛型解析失败）。 */
    private final DocumentRepository store;

    /** 当前仓储对应的实体类型。 */
    private final Class<T> type;

    protected AbstractDocumentRepository(DocumentRepository store, Class<T> type) {
        this.store = new TypeFilteredStore<>(store, type);
        this.type = type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T save(T entity) {
        return (T) store.save(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> saveAll(List<T> entities) {
        return (List<T>) store.saveAll((List<Object>) entities);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> findById(String id) {
        return (Optional<T>) store.findById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return (List<T>) store.findAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> findAllById(List<String> ids) {
        return (List<T>) store.findAllById(ids);
    }

    @Override
    public boolean existsById(String id) {
        return store.existsById(id);
    }

    @Override
    public long count() {
        return store.count();
    }

    @Override
    public long count(Query mongoQuery, Predicate<T> filter) {
        return store.count(mongoQuery, cast(filter));
    }

    @Override
    public PageData<T> page(Query mongoQuery, Predicate<T> filter, PageQuery pageQuery, SortOption... sorts) {
        PageData<Object> page = store.page(mongoQuery, cast(filter), pageQuery, sorts);
        List<T> records = cast(page.records());
        return new PageData<>(records, page.total());
    }

    @Override
    public List<T> find(Query mongoQuery, Predicate<T> filter, SortOption... sorts) {
        return cast(store.find(mongoQuery, cast(filter), sorts));
    }

    @Override
    public Optional<T> findOne(Query mongoQuery, Predicate<T> filter) {
        return cast(store.findOne(mongoQuery, cast(filter)));
    }

    @Override
    public boolean deleteById(String id) {
        return store.deleteById(id);
    }

    @Override
    public long deleteAllById(List<String> ids) {
        return store.deleteAllById(ids);
    }

    @Override
    public long deleteAll(Query mongoQuery, Predicate<T> filter) {
        return store.deleteAll(mongoQuery, cast(filter));
    }

    @Override
    public Class<T> entityType() {
        return type;
    }

    /** 底层通用仓储，供子类做更底层的操作。 */
    protected DocumentRepository store() {
        return store;
    }

    /**
     * 把 {@code Predicate<T>} 适配成底层需要的 {@code Predicate<Object>}。
     * 由于只会在同一实体类型的数据上调用，运行期安全。
     */
    @SuppressWarnings("unchecked")
    protected Predicate<Object> cast(Predicate<T> filter) {
        return (Predicate<Object>) filter;
    }

    /** 统一出口：把底层返回的 {@code Object}/{@code List<Object>} 转成目标类型。 */
    @SuppressWarnings("unchecked")
    protected <R> R cast(Object value) {
        return (R) value;
    }

    /**
     * 类型过滤装饰器：把通用仓储的读写限制在单一实体类型上。
     *
     * <p><b>为什么需要它：</b>内存版通用仓储把<b>所有实体类型</b>放在同一个扁平集合里
     * （{@code findAll()} 遍历全部 store），因此 {@code find}/{@code count}/{@code page}、
     * 甚至 {@code findAll()}/{@code findAllById()}/{@code count()}/{@code existsById()}
     * 都会跨越实体边界——一个 {@code Role} 询价的谓词会收到 {@code User} 实例并抛
     * {@code ClassCastException}，{@code count()} 也会把角色、薪资一起算进去。
     *
     * <p>MongoDB 版按集合隔离，本身没有此问题。这里在适配层统一按 {@code type} 过滤：
     * 既修掉内存版的越界读，也让两种实现语义一致（MongoDB 下类型过滤恒为真，仅有极小开销）。
     * 本类只约束新写的 5 组领域仓储；直接注入 {@code InMemoryDocumentRepository} 的代码
     * 仍会踩到该问题，需要另行修复底层实现。
     *
     * @param <T> 实体类型
     */
    private static final class TypeFilteredStore<T> implements DocumentRepository<Object> {

        private final DocumentRepository delegate;
        private final Class<T> type;

        private TypeFilteredStore(DocumentRepository delegate, Class<T> type) {
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public Object save(Object entity) {
            return delegate.save(entity);
        }

        @Override
        public List<Object> saveAll(List<Object> entities) {
            return delegate.saveAll(entities);
        }

        @Override
        public Optional<Object> findById(String id) {
            if (id == null) {
                return Optional.empty();
            }
            for (Object entity : delegate.findAll()) {
                if (type.isInstance(entity) && id.equals(idOf(entity))) {
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<Object> findAll() {
            List<Object> result = new ArrayList<>();
            for (Object entity : delegate.findAll()) {
                if (type.isInstance(entity)) {
                    result.add(entity);
                }
            }
            return result;
        }

        @Override
        public List<Object> findAllById(List<String> ids) {
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            List<Object> result = new ArrayList<>(ids.size());
            for (String id : ids) {
                findById(id).ifPresent(result::add);
            }
            return result;
        }

        @Override
        public boolean existsById(String id) {
            return findById(id).isPresent();
        }

        @Override
        public long count() {
            return findAll().size();
        }

        @Override
        public long count(Query mongoQuery, Predicate<Object> filter) {
            return findAll().stream().filter(guard(filter)).count();
        }

        @Override
        public PageData<Object> page(Query mongoQuery, Predicate<Object> filter, PageQuery pageQuery, SortOption... sorts) {
            List<Object> matched = find(mongoQuery, filter, sorts);
            int from = Math.min(pageQuery.offset(), matched.size());
            int to = Math.min(from + pageQuery.getSize(), matched.size());
            return new PageData<>(new ArrayList<>(matched.subList(from, to)), matched.size());
        }

        @Override
        public List<Object> find(Query mongoQuery, Predicate<Object> filter, SortOption... sorts) {
            List<Object> matched = new ArrayList<>();
            for (Object entity : findAll()) {
                if (guard(filter).test(entity)) {
                    matched.add(entity);
                }
            }
            if (matched.size() > 1) {
                matched.sort(comparator(sorts));
            }
            return matched;
        }

        @Override
        public Optional<Object> findOne(Query mongoQuery, Predicate<Object> filter) {
            for (Object entity : findAll()) {
                if (guard(filter).test(entity)) {
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean deleteById(String id) {
            if (id == null) {
                return false;
            }
            for (Object entity : findAll()) {
                if (id.equals(idOf(entity))) {
                    return delegate.deleteById(id);
                }
            }
            return false;
        }

        @Override
        public long deleteAllById(List<String> ids) {
            if (ids == null || ids.isEmpty()) {
                return 0L;
            }
            long deleted = 0L;
            for (String id : ids) {
                if (deleteById(id)) {
                    deleted++;
                }
            }
            return deleted;
        }

        @Override
        public long deleteAll(Query mongoQuery, Predicate<Object> filter) {
            long deleted = 0L;
            for (Object entity : find(null, filter)) {
                if (deleteById(idOf(entity))) {
                    deleted++;
                }
            }
            return deleted;
        }

        @Override
        public Class<Object> entityType() {
            return Object.class;
        }

        /** 读 {@code @Id} 字段的取值（与内存版通用仓储的反射工具同源）。 */
        private static String idOf(Object entity) {
            Object value = InMemoryIdSupport.idOf(entity);
            return value == null ? null : String.valueOf(value);
        }

        @SuppressWarnings("unchecked")
        private Predicate<Object> guard(Predicate<Object> filter) {
            Predicate<Object> byType = entity -> type.isInstance(entity);
            return filter == null ? byType : byType.and(filter);
        }

        @SuppressWarnings("unchecked")
        private static Comparator<Object> comparator(SortOption... sorts) {
            if (sorts == null || sorts.length == 0) {
                return (a, b) -> 0;
            }
            Comparator<Object> comparator = null;
            for (SortOption sort : sorts) {
                Comparator<Object> next = Comparator.comparing(
                        entity -> (Comparable<Object>) comparableValue(InMemoryIdSupport.readField(entity, sort.field())),
                        Comparator.nullsLast(Comparator.naturalOrder()));
                if (sort.desc()) {
                    next = next.reversed();
                }
                comparator = comparator == null ? next : comparator.thenComparing(next);
            }
            return comparator == null ? (a, b) -> 0 : comparator;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Comparable comparableValue(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Comparable<?> comparable) {
                return comparable;
            }
            return String.valueOf(value);
        }
    }

    /**
     * 反射读取 {@code @Id} / 排序字段的工具：与
     * {@code com.jyh.pms.storage.memory.InMemoryDocumentRepository.IdSupport} 的实现保持一致。
     *
     * <p>因为不能改动既有文件，这里复制了一份等价实现。注意既有版本在
     * 「字段值不可比较」时会走 {@code String.valueOf(value)} 分支，
     * 但返回类型是 {@code Comparable<Object>}，编译期会报
     * 「java.lang.String 无法转换为 java.lang.Comparable&lt;java.lang.Object&gt;」，
     * 所以本复刻版把返回类型放宽为原始 {@code Comparable}。
     */
    static final class InMemoryIdSupport {

        private static final Map<Class<?>, Field> ID_FIELDS = new ConcurrentHashMap<>();
        private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

        private InMemoryIdSupport() {
        }

        static String idOf(Object entity) {
            Object value = read(idField(entity.getClass()), entity);
            return value == null ? null : String.valueOf(value);
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

        private static Field idField(Class<?> type) {
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

        private static List<Field> allFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            Class<?> current = type;
            while (current != null && current != Object.class) {
                fields.addAll(List.of(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            return fields;
        }

        private static Object read(Field field, Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + field.getName(), e);
            }
        }
    }
}
