package com.jyh.pms.web.dto;

import java.math.BigDecimal;

/**
 * 首页看板概览指标。
 */
public record StatsOverviewView(
        long userTotal,
        long userEnabled,
        long userDisabled,
        long roleTotal,
        long positionTotal,
        long salaryTotal,
        BigDecimal salarySum,
        long departmentCount) {
}
