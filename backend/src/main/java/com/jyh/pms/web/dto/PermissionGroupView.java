package com.jyh.pms.web.dto;

/**
 * 权限分组视图：{@code {group, items:[{code,label}]}}。
 */
public record PermissionGroupView(String group, java.util.List<PermissionItem> items) {

    /** 单项权限。 */
    public record PermissionItem(String code, String label) {
    }
}
