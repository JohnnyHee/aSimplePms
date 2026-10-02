package com.jyh.pms.web;

import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.storage.SortOption;

import java.util.Set;

/**
 * 人员列表筛选条件（page/size/sort 由 {@link PageQuery} 承载）。
 */
public class UserQuery {

    /** 允许排序的字段白名单，避免任意字段穿透到存储层。 */
    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "updatedAt", "username", "name", "department", "status", "lastLoginAt", "age");

    private String keyword;
    private String roleId;
    private String positionId;
    private String status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** 把统一的分页排序参数转换成存储层排序选项，非白名单字段回退为 createdAt。 */
    public static SortOption[] sortOptions(PageQuery pageQuery) {
        String field = pageQuery.sortField();
        if (!SORTABLE.contains(field)) {
            field = "createdAt";
        }
        return new SortOption[]{SortOption.of(field, pageQuery.sortDescending())};
    }
}
