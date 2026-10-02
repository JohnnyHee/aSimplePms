package com.jyh.pms.storage;

/**
 * 排序选项。
 *
 * @param field 字段名（实体属性名）
 * @param desc  是否倒序
 */
public record SortOption(String field, boolean desc) {

    public static SortOption of(String field, boolean desc) {
        return new SortOption(field, desc);
    }

    public static SortOption desc(String field) {
        return new SortOption(field, true);
    }

    public static SortOption asc(String field) {
        return new SortOption(field, false);
    }
}
