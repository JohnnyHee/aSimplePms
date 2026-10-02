package com.jyh.pms.web.dto;

import com.jyh.pms.web.MenuCatalog;

import java.util.List;

/**
 * {@code GET /api/auth/permissions} 的响应：权限编码、角色编码与可见菜单。
 */
public record UserPermissionsView(List<String> permissions, List<String> roles, List<MenuCatalog.MenuItem> menus) {
}
