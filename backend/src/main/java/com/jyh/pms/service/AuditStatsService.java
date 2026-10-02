package com.jyh.pms.service;

import com.jyh.pms.core.error.BusinessException;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.domain.AuditLog;
import com.jyh.pms.domain.PayType;
import com.jyh.pms.domain.Salary;
import com.jyh.pms.domain.SalaryStatus;
import com.jyh.pms.domain.User;
import com.jyh.pms.domain.UserStatus;
import com.jyh.pms.storage.AuditLogRepository;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.web.dto.AuditLogView;
import com.jyh.pms.web.dto.NameValue;
import com.jyh.pms.web.dto.StatsOverviewView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 审计与统计服务：审计日志的分页检索/清空，以及首页看板所需的聚合指标。
 */
@Service
public class AuditStatsService {

    private static final Logger log = LoggerFactory.getLogger(AuditStatsService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SalaryService salaryService;
    private final RoleService roleService;
    private final PositionService positionService;

    public AuditStatsService(AuditLogRepository auditLogRepository, UserRepository userRepository,
                             SalaryService salaryService, RoleService roleService,
                             PositionService positionService) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.salaryService = salaryService;
        this.roleService = roleService;
        this.positionService = positionService;
    }

    /** 分页查询审计日志。 */
    public PageData<AuditLogView> page(PageQuery pageQuery, String username, String action, String outcome,
                                       Instant from, Instant to) {
        SortOption sort = new SortOption(pageQuery.sortField(), pageQuery.sortDescending());
        if (!List.of("createdAt", "username", "action", "outcome", "elapsedMs").contains(sort.field())) {
            sort = SortOption.desc("createdAt");
        }
        Predicate<AuditLog> filter = auditLog -> {
            if (username != null && !username.isBlank()
                    && (auditLog.getUsername() == null || !auditLog.getUsername().contains(username.trim()))) {
                return false;
            }
            if (action != null && !action.isBlank() && !action.trim().equals(auditLog.getAction())) {
                return false;
            }
            if (outcome != null && !outcome.isBlank() && !outcome.trim().equalsIgnoreCase(auditLog.getOutcome())) {
                return false;
            }
            Instant createdAt = auditLog.getCreatedAt();
            if (from != null && (createdAt == null || createdAt.isBefore(from))) {
                return false;
            }
            return to == null || (createdAt != null && !createdAt.isAfter(to));
        };
        PageData<AuditLog> page = auditLogRepository.page(null, filter, pageQuery, sort);
        return new PageData<>(page.records().stream().map(this::toView).toList(), page.total());
    }

    /** 最近的审计日志（首页看板用）。 */
    public List<AuditLogView> recent(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(1);
        pageQuery.setSize(size);
        pageQuery.setSort("createdAt,desc");
        return auditLogRepository.page(null, auditLog -> true, pageQuery, SortOption.desc("createdAt"))
                .records().stream().map(this::toView).toList();
    }

    /** 清空审计日志。 */
    public long clear() {
        long total = auditLogRepository.count();
        if (total > 0) {
            auditLogRepository.deleteAll(null, auditLog -> true);
        }
        log.warn("审计日志已被清空，共删除 {} 条", total);
        return total;
    }

    /** 首页概览指标。 */
    public StatsOverviewView overview() {
        List<User> users = userRepository.findAll();
        long enabled = users.stream().filter(user -> user.getStatus() == null || user.getStatus() == UserStatus.ENABLED).count();
        BigDecimal salarySum = salaryService.sumActive();
        long departments = users.stream()
                .map(User::getDepartment)
                .filter(department -> department != null && !department.isBlank())
                .distinct()
                .count();
        return new StatsOverviewView(
                users.size(),
                enabled,
                users.size() - enabled,
                roleService.page(defaultPageQuery()).total(),
                positionService.count(),
                salaryService.count(),
                salarySum == null ? BigDecimal.ZERO : salarySum,
                departments);
    }

    /** 按部门统计人数，返回 [{name,value}]。 */
    public List<NameValue> usersByDepartment() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (User user : userRepository.findAll()) {
            String department = user.getDepartment();
            String key = department == null || department.isBlank() ? "未分配" : department;
            counts.merge(key, 1L, Long::sum);
        }
        return toNameValues(counts);
    }

    /** 按薪资类型统计条数，返回 [{name,value}]。 */
    public List<NameValue> payTypeDistribution() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map.Entry<PayType, Long> entry : salaryService.countByPayType().entrySet()) {
            counts.put(entry.getKey().label(), entry.getValue());
        }
        return toNameValues(counts);
    }

    private static List<NameValue> toNameValues(Map<String, Long> counts) {
        List<NameValue> result = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> result.add(new NameValue(entry.getKey(), entry.getValue())));
        return result;
    }

    private static PageQuery defaultPageQuery() {
        PageQuery query = new PageQuery();
        query.setPage(1);
        query.setSize(1);
        return query;
    }

    private AuditLogView toView(AuditLog auditLog) {
        return new AuditLogView(auditLog.getId(), auditLog.getUsername(), auditLog.getUid(), auditLog.getAction(),
                auditLog.getMethod(), auditLog.getPath(), auditLog.getIp(), auditLog.getOutcome(),
                auditLog.getDetail(), auditLog.getElapsedMs(), auditLog.getCreatedAt());
    }

    /** 供控制器把查询参数里的字符串时间解析为 Instant。 */
    public static Instant parseInstant(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (RuntimeException ignored) {
            // 继续尝试日期时间（无时区）格式
        }
        try {
            return java.time.LocalDateTime.parse(value.trim().replace(' ', 'T'))
                    .atZone(java.time.ZoneId.systemDefault()).toInstant();
        } catch (RuntimeException e) {
            throw BusinessException.badRequest(label + "格式应为 ISO-8601 时间");
        }
    }

}
