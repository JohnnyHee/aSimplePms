package com.jyh.pms.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 薪资视图对象。
 */
public record SalaryView(
        String id,
        String uid,
        String userName,
        String positionId,
        String positionName,
        BigDecimal amount,
        String payType,
        String payTypeLabel,
        String status,
        String statusLabel,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String remark,
        Instant createdAt) {
}
