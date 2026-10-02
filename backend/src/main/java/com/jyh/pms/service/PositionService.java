package com.jyh.pms.service;

import com.jyh.pms.core.error.BusinessException;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.domain.Position;
import com.jyh.pms.domain.User;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.web.dto.PositionView;
import com.jyh.pms.web.dto.req.PositionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 职位服务：职位 CRUD、下拉选项、被人员引用统计。
 */
@Service
public class PositionService {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    private final PositionRepository positionRepository;
    private final UserRepository userRepository;

    public PositionService(PositionRepository positionRepository, UserRepository userRepository) {
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
    }

    /** 分页查询职位。 */
    public PageData<PositionView> page(PageQuery pageQuery) {
        SortOption sort = new SortOption(pageQuery.sortField(), pageQuery.sortDescending());
        if (!List.of("createdAt", "updatedAt", "code", "name", "sortOrder", "level").contains(sort.field())) {
            sort = SortOption.asc("sortOrder");
        }
        PageData<Position> page = positionRepository.page(null, position -> true, pageQuery, sort);
        Map<String, Long> userCounts = userCountsByPosition();
        List<PositionView> records = page.records().stream()
                .map(position -> toView(position, userCounts.getOrDefault(position.getId(), 0L)))
                .toList();
        return new PageData<>(records, page.total());
    }

    /** 全部职位（下拉选项），按 sortOrder 升序。 */
    public List<PositionView> options() {
        Map<String, Long> userCounts = userCountsByPosition();
        return positionRepository.findAllOrderBySortOrder().stream()
                .map(position -> toView(position, userCounts.getOrDefault(position.getId(), 0L)))
                .toList();
    }

    /** 职位详情。 */
    public PositionView get(String id) {
        Position position = require(id);
        return toView(position, userCountsByPosition().getOrDefault(id, 0L));
    }

    /** 新增职位。 */
    public PositionView create(PositionRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (positionRepository.existsByCode(code)) {
            throw BusinessException.conflict("职位编码已存在: " + code);
        }
        Instant now = Instant.now();
        Position position = new Position();
        apply(position, request);
        position.setCode(code);
        position.setCreatedAt(now);
        position.setUpdatedAt(now);
        Position saved = positionRepository.save(position);
        log.info("新增职位 code={}", code);
        return toView(saved, 0L);
    }

    /** 修改职位。 */
    public PositionView update(String id, PositionRequest request) {
        Position position = require(id);
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (!code.equals(position.getCode()) && positionRepository.existsByCode(code)) {
            throw BusinessException.conflict("职位编码已存在: " + code);
        }
        apply(position, request);
        position.setCode(code);
        position.setUpdatedAt(Instant.now());
        positionRepository.save(position);
        return toView(position, userCountsByPosition().getOrDefault(id, 0L));
    }

    /** 删除职位：被人员引用时禁止删除。 */
    public void delete(String id) {
        Position position = require(id);
        long users = userRepository.countByPositionId(id);
        if (users > 0) {
            throw BusinessException.business("该职位仍关联 " + users + " 名人员，无法删除");
        }
        positionRepository.deleteById(id);
        log.info("删除职位 code={}", position.getCode());
    }

    /** 职位总数（统计用）。 */
    public long count() {
        return positionRepository.count();
    }

    private void apply(Position position, PositionRequest request) {
        position.setName(request.getName().trim());
        position.setLevel(trimToNull(request.getLevel()));
        position.setDescription(trimToNull(request.getDescription()));
        if (request.getSortOrder() != null) {
            position.setSortOrder(request.getSortOrder());
        }
    }

    private Position require(String id) {
        return positionRepository.findById(id).orElseThrow(() -> BusinessException.notFound("职位不存在: " + id));
    }

    /** 职位 id -> 关联人数。 */
    Map<String, Long> userCountsByPosition() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (User user : userRepository.findAll()) {
            String positionId = user.getPositionId();
            if (positionId != null && !positionId.isBlank()) {
                counts.merge(positionId, 1L, Long::sum);
            }
        }
        return counts;
    }

    private PositionView toView(Position position, long userCount) {
        return new PositionView(position.getId(), position.getCode(), position.getName(), position.getLevel(),
                position.getDescription(), position.getSortOrder(), userCount, position.getCreatedAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 便捷方法：按 id 批量取名称映射。 */
    public Map<String, String> nameMap(List<String> ids) {
        Map<String, String> map = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        positionRepository.findAllById(ids).stream().filter(Objects::nonNull)
                .forEach(position -> map.put(position.getId(), position.getName()));
        return map;
    }
}
