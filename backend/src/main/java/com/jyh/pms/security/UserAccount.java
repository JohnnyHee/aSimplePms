package com.jyh.pms.security;

import java.util.List;

/**
 * 认证所需的用户账号视图（不包含密码以外的敏感信息）。
 *
 * @param uid         用户 id
 * @param username    登录名
 * @param displayName 展示名
 * @param password    BCrypt 密码摘要
 * @param enabled     是否启用
 * @param locked      是否被锁定
 * @param roles       角色编码
 * @param permissions 权限编码
 */
public record UserAccount(String uid, String username, String displayName, String password,
                          boolean enabled, boolean locked, List<String> roles, List<String> permissions) {
}
