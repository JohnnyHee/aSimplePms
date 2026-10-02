package com.jyh.pms.service;

import com.jyh.pms.core.error.BusinessException;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.domain.PayType;
import com.jyh.pms.domain.Position;
import com.jyh.pms.domain.Salary;
import com.jyh.pms.domain.SalaryStatus;
import com.jyh.pms.domain.User;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.SalaryRepository;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.web.SalaryQuery;
import com.jyh.pms.web.dto.SalaryView;
import com.jyh.pms.web.dto.req.SalaryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 薪资服务：薪资记录的分页检索、增删改，以及"同一人只能有一条生效薪资"的自动归档。
 */
@Service
public class SalaryService {

    private static final Logger log = LoggerFactory.getLogger(SalaryService.class);

    private final SalaryRepository salaryRepository;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;

    public SalaryService(SalaryRepository salaryRepository, UserRepository userRepository,
                         PositionRepository positionRepository) {
        this.salaryRepository = salaryRepository;
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
    }

    /** 分页查询薪资记录。 */
    public PageData<SalaryView> page(PageQuery pageQuery, SalaryQuery query) {
        SortOption sort = new SortOption(pageQuery.sortField(), pageQuery.sortDescending());
        if (!List.of("createdAt", "updatedAt", "amount", "effectiveFrom", "payType", "status").contains(sort.field())) {
            sort = SortOption.desc("createdAt");
        }
        PageData<Salary> page = salaryRepository.page(null, filter(query), pageQuery, sort);
        Map<String, String> userNames = userNames(page.records().stream().map(Salary::getUid).distinct().toList());
        Map<String, String> positionNames = positionNames(
                page.records().stream().map(Salary::getPositionId).filter(Objects::nonNull).distinct().toList());
        List<SalaryView> records = page.records().stream()
                .map(salary -> toView(salary, userNames, positionNames))
                .toList();
        return new PageData<>(records, page.total());
    }

    /** 新增薪资记录。 */
    public SalaryView create(SalaryRequest request) {
        User user = requireUser(request.getUid());
        validatePosition(request.getPositionId());
        PayType payType = parsePayType(request.getPayType());
        SalaryStatus status = parseStatus(request.getStatus(), SalaryStatus.ACTIVE);
        Salary salary = new Salary();
        salary.setUid(user.getUid());
        salary.setPositionId(trimToNull(request.getPositionId()));
        salary.setAmount(request.getAmount());
        salary.setPayType(payType);
        salary.setStatus(status);
        salary.setEffectiveFrom(parseDate(request.getEffectiveFrom(), "生效日期"));
        salary.setEffectiveTo(parseDate(request.getEffectiveTo(), "失效日期"));
        salary.setRemark(trimToNull(request.getRemark()));
        salary.setOperator(SecurityUtils.currentUsername());
        Instant now = Instant.now();
        salary.setCreatedAt(now);
        salary.setUpdatedAt(now);
        Salary saved = salaryRepository.save(salary);
        archiveOtherActive(saved);
        log.info("新增薪资记录 id={}, uid={}, 类型={}, 金额={}", saved.getId(), saved.getUid(), saved.getPayType(), saved.getAmount());
        return get(saved.getId());
    }

    /** 修改薪资记录。 */
    public SalaryView update(String id, SalaryRequest request) {
        Salary salary = require(id);
        if (!salary.getUid().equals(request.getUid())) {
            throw BusinessException.badRequest("不允许修改薪资记录所属人员");
        }
        validatePosition(request.getPositionId());
        salary.setPositionId(trimToNull(request.getPositionId()));
        salary.setAmount(request.getAmount());
        salary.setPayType(parsePayType(request.getPayType()));
        salary.setStatus(parseStatus(request.getStatus(), salary.getStatus()));
        salary.setEffectiveFrom(parseDate(request.getEffectiveFrom(), "生效日期"));
        salary.setEffectiveTo(parseDate(request.getEffectiveTo(), "失效日期"));
        salary.setRemark(trimToNull(request.getRemark()));
        salary.setOperator(SecurityUtils.currentUsername());
        salary.setUpdatedAt(Instant.now());
        salaryRepository.save(salary);
        archiveOtherActive(salary);
        return get(id);
    }

    /** 删除薪资记录。 */
    public void delete(String id) {
        Salary salary = require(id);
        salaryRepository.deleteById(id);
        log.info("删除薪资记录 id={}, uid={}", id, salary.getUid());
    }

    /** 查询某人的调薪历史。 */
    public List<Salary> history(String uid) {
        return salaryRepository.findByUidOrderByEffectiveFromDesc(uid);
    }

    private SalaryView get(String id) {
        Salary salary = require(id);
        return toView(salary, userNames(List.of(salary.getUid())),
                positionNames(salary.getPositionId() == null ? List.of() : List.of(salary.getPositionId())));
    }

    /** 同一个人只保留一条 ACTIVE 记录，其余自动归档为 CLOSED。 */
    private void archiveOtherActive(Salary saved) {
        if (saved.getStatus() != SalaryStatus.ACTIVE) {
            return;
        }
        List<Salary> actives = salaryRepository.find(null, salary -> saved.getUid().equals(salary.getUid())
                && salary.getStatus() == SalaryStatus.ACTIVE && !saved.getId().equals(salary.getId()));
        if (actives.isEmpty()) {
            return;
        }
        for (Salary salary : actives) {
            salary.setStatus(SalaryStatus.CLOSED);
            salary.setEffectiveTo(saved.getEffectiveFrom() == null ? LocalDate.now() : saved.getEffectiveFrom());
            salary.setUpdatedAt(Instant.now());
            salaryRepository.save(salary);
        }
        log.info("人员 {} 新增生效薪资，自动归档 {} 条历史记录", saved.getUid(), actives.size());
    }

    private User requireUser(String uid) {
        return userRepository.findById(uid).orElseThrow(() -> BusinessException.badRequest("人员不存在: " + uid));
    }

    private Salary require(String id) {
        return salaryRepository.findById(id).orElseThrow(() -> BusinessException.notFound("薪资记录不存在: " + id));
    }

    private void validatePosition(String positionId) {
        if (positionId == null || positionId.isBlank()) {
            return;
        }
        if (positionRepository.findById(positionId).isEmpty()) {
            throw BusinessException.badRequest("职位不存在: " + positionId);
        }
    }

    static Predicate<Salary> filter(SalaryQuery query) {
        if (query == null) {
            return salary -> true;
        }
        String uid = trimToNull(query.getUid());
        PayType payType = query.getPayType() == null || query.getPayType().isBlank()
                ? null : parsePayType(query.getPayType());
        SalaryStatus status = query.getStatus() == null || query.getStatus().isBlank()
                ? null : parseStatus(query.getStatus(), null);
        return salary -> {
            if (uid != null && !uid.equals(salary.getUid())) {
                return false;
            }
            if (payType != null && salary.getPayType() != payType) {
                return false;
            }
            return status == null || salary.getStatus() == status;
        };
    }

    private Map<String, String> userNames(List<String> uids) {
        Map<String, String> map = new LinkedHashMap<>();
        if (uids == null || uids.isEmpty()) {
            return map;
        }
        userRepository.findAllById(uids).forEach(user ->
                map.put(user.getUid(), user.getName() == null ? user.getUsername() : user.getName()));
        return map;
    }

    private Map<String, String> positionNames(List<String> positionIds) {
        Map<String, String> map = new LinkedHashMap<>();
        if (positionIds == null || positionIds.isEmpty()) {
            return map;
        }
        for (Position position : positionRepository.findAllById(positionIds)) {
            map.put(position.getId(), position.getName());
        }
        return map;
    }

    private SalaryView toView(Salary salary, Map<String, String> userNames, Map<String, String> positionNames) {
        PayType payType = salary.getPayType() == null ? PayType.MONTHLY : salary.getPayType();
        SalaryStatus status = salary.getStatus() == null ? SalaryStatus.ACTIVE : salary.getStatus();
        return new SalaryView(
                salary.getId(),
                salary.getUid(),
                userNames.get(salary.getUid()),
                salary.getPositionId(),
                salary.getPositionId() == null ? null : positionNames.get(salary.getPositionId()),
                salary.getAmount(),
                payType.name(),
                payType.label(),
                status.name(),
                status.label(),
                salary.getEffectiveFrom(),
                salary.getEffectiveTo(),
                salary.getRemark(),
                salary.getCreatedAt());
    }

    private static PayType parsePayType(String value) {
        try {
            return PayType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw BusinessException.badRequest("薪资类型不正确: " + value);
        }
    }

    private static SalaryStatus parseStatus(String value, SalaryStatus fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return SalaryStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw BusinessException.badRequest("薪资状态不正确: " + value);
        }
    }

    private static LocalDate parseDate(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException e) {
            throw BusinessException.badRequest(label + "格式应为 yyyy-MM-dd");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 统计用：薪资记录总数。 */
    public long count() {
        return salaryRepository.count();
    }

    /** 统计用：生效中薪资总额。 */
    public BigDecimal sumActive() {
        return salaryRepository.find(null, salary -> salary.getStatus() == SalaryStatus.ACTIVE).stream()
                .map(Salary::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 统计用：按薪资类型分组计数。 */
    public Map<PayType, Long> countByPayType() {
        Map<PayType, Long> counts = new LinkedHashMap<>();
        for (Salary salary : salaryRepository.findAll()) {
            PayType payType = salary.getPayType() == null ? PayType.MONTHLY : salary.getPayType();
            counts.merge(payType, 1L, Long::sum);
        }
        return counts;
    }
}
