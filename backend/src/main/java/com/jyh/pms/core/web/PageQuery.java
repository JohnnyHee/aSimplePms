package com.jyh.pms.core.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 统一的分页/排序请求参数。前端以 query string 传递：{@code ?page=1&size=20&sort=createdAt,desc}。
 */
public class PageQuery {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;
    private String sort = "createdAt,desc";

    public PageQuery() {
    }

    public PageQuery(int page, int size, String sort) {
        setPage(page);
        setSize(size);
        setSort(sort);
    }

    public int getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(Integer size) {
        if (size == null || size < 1) {
            this.size = DEFAULT_SIZE;
        } else {
            this.size = Math.min(size, MAX_SIZE);
        }
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort.trim();
    }

    /** 跳过多少条。 */
    public int offset() {
        return (page - 1) * size;
    }

    /** 排序字段名（已去掉方向）。 */
    public String sortField() {
        List<String> parts = sortParts();
        return parts.get(0);
    }

    /** 是否倒序。 */
    public boolean sortDescending() {
        List<String> parts = sortParts();
        return parts.size() > 1 && "desc".equals(parts.get(1));
    }

    private List<String> sortParts() {
        String[] split = sort.split(",");
        List<String> parts = new ArrayList<>(2);
        String field = split[0].trim();
        parts.add(field.isEmpty() ? "createdAt" : field);
        if (split.length > 1) {
            parts.add(split[1].trim().toLowerCase(Locale.ROOT));
        }
        return parts;
    }
}
