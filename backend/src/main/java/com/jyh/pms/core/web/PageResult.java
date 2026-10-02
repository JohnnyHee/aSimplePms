package com.jyh.pms.core.web;

import java.util.List;

/**
 * 分页查询结果（服务端 -> 前端）。
 *
 * @param records 当前页数据
 * @param total   总记录数
 * @param page    当前页码，从 1 开始
 * @param size    每页条数
 */
public record PageResult<T>(List<T> records, long total, int page, int size) {

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
