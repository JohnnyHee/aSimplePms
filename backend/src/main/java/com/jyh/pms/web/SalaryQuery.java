package com.jyh.pms.web;

import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.storage.SortOption;

import java.util.Set;

/**
 * 薪资列表筛选条件（page/size/sort 由 {@link PageQuery} 承载）。
 */
public class SalaryQuery {

    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "updatedAt", "amount", "effectiveFrom", "payType", "status");

    private String uid;
    private String payType;
    private String status;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** 排序白名单校验后返回存储层排序选项。 */
    public static SortOption[] sortOptions(PageQuery pageQuery) {
        String field = pageQuery.sortField();
        if (!SORTABLE.contains(field)) {
            field = "createdAt";
        }
        return new SortOption[]{SortOption.of(field, pageQuery.sortDescending())};
    }
}
