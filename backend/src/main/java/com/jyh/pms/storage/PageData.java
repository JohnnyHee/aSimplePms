package com.jyh.pms.storage;

import java.util.List;

/**
 * 统一的分页查询结果（存储层内部使用）。
 *
 * @param records 当前页数据
 * @param total   命中总数
 */
public record PageData<T>(List<T> records, long total) {

    public static <T> PageData<T> empty() {
        return new PageData<>(List.of(), 0L);
    }
}
