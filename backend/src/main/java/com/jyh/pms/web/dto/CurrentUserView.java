package com.jyh.pms.web.dto;

/**
 * 登录用户信息（含角色与权限，供前端菜单/按钮控制）。
 */
public record CurrentUserView(
        String uid,
        String username,
        String displayName,
        java.util.List<String> roles,
        java.util.List<String> permissions) {
}
