package com.jyh.pms.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户身份（无状态：完全由 JWT 中的声明还原，不查库）。
 *
 * @param uid         用户 id
 * @param username    登录名
 * @param roles       角色编码
 * @param permissions 权限编码
 */
public record AuthUser(String uid, String username, List<String> roles, List<String> permissions) {

    /** 角色统一加 {@code ROLE_} 前缀，权限原样使用，二者都作为 Spring Security 的 authority。 */
    public Collection<GrantedAuthority> authorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return authorities;
    }
}
