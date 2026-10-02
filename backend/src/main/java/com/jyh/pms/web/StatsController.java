package com.jyh.pms.web;

import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.service.AuditStatsService;
import com.jyh.pms.web.dto.AuditLogView;
import com.jyh.pms.web.dto.NameValue;
import com.jyh.pms.web.dto.StatsOverviewView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页看板统计接口。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final AuditStatsService auditStatsService;

    public StatsController(AuditStatsService auditStatsService) {
        this.auditStatsService = auditStatsService;
    }

    /** 概览指标。 */
    @GetMapping("/overview")
    public ApiResponse<StatsOverviewView> overview() {
        return ApiResponse.ok(auditStatsService.overview());
    }

    /** 按部门统计人数。 */
    @GetMapping("/users-by-department")
    @PreAuthorize("hasAnyAuthority('user:view','audit:view')")
    public ApiResponse<List<NameValue>> usersByDepartment() {
        return ApiResponse.ok(auditStatsService.usersByDepartment());
    }

    /** 按薪资类型统计条数。 */
    @GetMapping("/pay-type-distribution")
    @PreAuthorize("hasAnyAuthority('salary:view','audit:view')")
    public ApiResponse<List<NameValue>> payTypeDistribution() {
        return ApiResponse.ok(auditStatsService.payTypeDistribution());
    }

    /** 最近的审计动态。 */
    @GetMapping("/recent-audit-logs")
    @PreAuthorize("hasAuthority('audit:view')")
    public ApiResponse<List<AuditLogView>> recentAuditLogs(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(auditStatsService.recent(limit));
    }
}
