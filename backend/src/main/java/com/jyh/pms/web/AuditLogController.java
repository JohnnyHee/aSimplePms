package com.jyh.pms.web;

import com.jyh.pms.core.audit.Audited;
import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.PageResult;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.service.AuditStatsService;
import com.jyh.pms.web.dto.AuditLogView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志接口。
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditStatsService auditStatsService;

    public AuditLogController(AuditStatsService auditStatsService) {
        this.auditStatsService = auditStatsService;
    }

    /** 分页查询审计日志。 */
    @GetMapping
    @PreAuthorize("hasAuthority('audit:view')")
    public ApiResponse<PageResult<AuditLogView>> page(PageQuery pageQuery,
                                                      @RequestParam(required = false) String username,
                                                      @RequestParam(required = false) String action,
                                                      @RequestParam(required = false) String outcome,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to) {
        PageResult<AuditLogView> result = SecurityUtils.toPage(
                auditStatsService.page(pageQuery, username, action, outcome,
                        AuditStatsService.parseInstant(from, "开始时间"),
                        AuditStatsService.parseInstant(to, "结束时间")),
                pageQuery);
        return ApiResponse.ok(result);
    }

    /** 清空审计日志。 */
    @DeleteMapping
    @PreAuthorize("hasAuthority('audit:clear')")
    @Audited(action = "清空审计日志", recordDetail = false)
    public ApiResponse<Long> clear() {
        return ApiResponse.ok(auditStatsService.clear());
    }
}
