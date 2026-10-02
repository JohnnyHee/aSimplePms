package com.jyh.pms.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * 人员视图对象（对外 JSON）。绝不含 password。
 */
public record UserView(
        String uid,
        String username,
        String name,
        String nickname,
        String department,
        String email,
        String phone,
        String avatar,
        String remark,
        Integer age,
        String status,
        String statusLabel,
        List<String> roleIds,
        List<String> roleNames,
        String positionId,
        String positionName,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt) {
}
