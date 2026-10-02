package com.jyh.pms.web;

import com.jyh.pms.domain.Permission;

import java.util.List;

/**
 * 后台菜单目录（与前端固定路由一一对应），登录后按用户权限过滤返回。
 */
public final class MenuCatalog {

    private MenuCatalog() {
    }

    /** 全部菜单项，{@code code} 对应前端路由 name，{@code permission} 为可见性所需权限。 */
    public static List<MenuItem> all() {
        return List.of(
                new MenuItem("dashboard", "首页看板", null),
                new MenuItem("users", "人员管理", Permission.USER_VIEW.code()),
                new MenuItem("roles", "角色权限", Permission.ROLE_VIEW.code()),
                new MenuItem("positions", "职位管理", Permission.POSITION_VIEW.code()),
                new MenuItem("salaries", "薪资管理", Permission.SALARY_VIEW.code()),
                new MenuItem("auditLogs", "审计日志", Permission.AUDIT_VIEW.code()),
                new MenuItem("profile", "个人中心", null));
    }

    /**
     * 菜单项。
     *
     * @param code       菜单/路由标识
     * @param name       展示名
     * @param permission 所需权限（null 表示所有登录用户可见）
     */
    public record MenuItem(String code, String name, String permission) {
    }
}
