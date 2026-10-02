package com.jyh.pms.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * 角色视图对象。
 */
public record RoleView(
        String id,
        String code,
        String name,
        String description,
        List<String> permissions,
        boolean builtIn,
        long userCount,
        Instant createdAt) {
}
