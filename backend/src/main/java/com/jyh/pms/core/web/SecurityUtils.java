package com.jyh.pms.core.web;

import com.jyh.pms.storage.PageData;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当前登录用户（由 JWT 还原的无状态身份）相关的工具方法。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户名，未登录返回 null。 */
    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.jyh.pms.security.AuthUser authUser) {
            return authUser.username();
        }
        return null;
    }

    /** 当前登录用户 id，未登录返回 null。 */
    public static String currentUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.jyh.pms.security.AuthUser authUser) {
            return authUser.uid();
        }
        return null;
    }

    /** 当前登录用户主体，未登录返回 null。 */
    public static com.jyh.pms.security.AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof com.jyh.pms.security.AuthUser authUser ? authUser : null;
    }

    /** 当前用户是否持有指定权限。 */
    public static boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains(permission);
    }

    /** 当前用户是否为系统管理员。 */
    public static boolean isAdmin() {
        return hasPermission(com.jyh.pms.domain.Permission.AUDIT_CLEAR.code());
    }

    /** 把存储层分页结果包装为对外分页结构。 */
    public static <T> PageResult<T> toPage(PageData<T> page, PageQuery query) {
        long total = page.total();
        List<T> records = page.records() == null ? List.of() : page.records();
        return new PageResult<>(records, total, query.getPage(), query.getSize());
    }
}
