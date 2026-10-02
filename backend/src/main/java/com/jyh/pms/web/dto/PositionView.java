package com.jyh.pms.web.dto;

import java.time.Instant;

/**
 * 职位视图对象。
 */
public record PositionView(
        String id,
        String code,
        String name,
        /** 职级，例如 P5 / M2。 */
        String level,
        String description,
        int sortOrder,
        long userCount,
        Instant createdAt) {
}
