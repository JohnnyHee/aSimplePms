package com.jyh.pms.service;

import com.jyh.pms.core.error.BusinessException;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.domain.Permission;
import com.jyh.pms.domain.Role;
import com.jyh.pms.domain.User;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.RoleRepository;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.web.dto.PermissionGroupView;
import com.jyh.pms.web.dto.RoleView;
import com.jyh.pms.web.dto.req.RoleCreateRequest;
import com.jyh.pms.web.dto.req.RoleUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色与权限服务：角色 CRUD（内置角色受保护）、权限目录查询、角色引用统计。
 */
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /** 分页查询角色。 */
    public PageData<RoleView> page(PageQuery pageQuery) {
        SortOption sort = new SortOption(pageQuery.sortField(), pageQuery.sortDescending());
        if (!List.of("createdAt", "updatedAt", "code", "name").contains(sort.field())) {
            sort = SortOption.desc("createdAt");
        }
        PageData<Role> page = roleRepository.page(null, role -> true, pageQuery, sort);
        Map<String, Long> userCounts = userCountsByRole();
        List<RoleView> records = page.records().stream()
                .map(role -> toView(role, userCounts.getOrDefault(role.getId(), 0L)))
                .toList();
        return new PageData<>(records, page.total());
    }

    /** 全部角色（下拉选项）。 */
    public List<RoleView> options() {
        Map<String, Long> userCounts = userCountsByRole();
        return roleRepository.find(null, role -> true, SortOption.asc("code")).stream()
                .map(role -> toView(role, userCounts.getOrDefault(role.getId(), 0L)))
                .toList();
    }

    /** 角色详情。 */
    public RoleView get(String id) {
        Role role = require(id);
        return toView(role, userCountsByRole().getOrDefault(id, 0L));
    }

    /** 新增角色。 */
    public RoleView create(RoleCreateRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (roleRepository.existsByCode(code)) {
            throw BusinessException.conflict("角色编码已存在: " + code);
        }
        Set<String> permissions = validatePermissions(request.getPermissions());
        Instant now = Instant.now();
        Role role = new Role();
        role.setCode(code);
        role.setName(request.getName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        role.setPermissions(List.copyOf(permissions));
        role.setBuiltIn(false);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        Role saved = roleRepository.save(role);
        log.info("新增角色 code={}, 权限 {} 项", code, permissions.size());
        return toView(saved, 0L);
    }

    /** 修改角色：内置角色不允许改权限。 */
    public RoleView update(String id, RoleUpdateRequest request) {
        Role role = require(id);
        Set<String> permissions = validatePermissions(request.getPermissions());
        if (role.isBuiltIn()) {
            Set<String> current = new LinkedHashSet<>(role.getPermissions() == null ? List.of() : role.getPermissions());
            if (!current.equals(permissions)) {
                throw BusinessException.business("内置角色的权限不可修改");
            }
        }
        role.setName(request.getName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        role.setPermissions(List.copyOf(permissions));
        role.setUpdatedAt(Instant.now());
        roleRepository.save(role);
        return toView(role, userCountsByRole().getOrDefault(id, 0L));
    }

    /** 删除角色：内置角色或被人员引用时禁止删除。 */
    public void delete(String id) {
        Role role = require(id);
        if (role.isBuiltIn()) {
            throw BusinessException.business("内置角色不可删除");
        }
        long users = userRepository.count(null, user ->
                user.getRoleIds() != null && user.getRoleIds().contains(id));
        if (users > 0) {
            throw BusinessException.business("该角色仍被 " + users + " 名人员使用，无法删除");
        }
        roleRepository.deleteById(id);
        log.info("删除角色 code={}", role.getCode());
    }

    /** 权限目录（按分组）。 */
    public List<PermissionGroupView> permissionGroups() {
        Map<String, List<PermissionGroupView.PermissionItem>> groups = new LinkedHashMap<>();
        Arrays.stream(Permission.values()).forEach(permission ->
                groups.computeIfAbsent(permission.group(), key -> new ArrayList<>())
                        .add(new PermissionGroupView.PermissionItem(permission.code(), permission.label())));
        return groups.entrySet().stream()
                .map(entry -> new PermissionGroupView(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    /** 全部权限编码（按枚举顺序）。 */
    public List<String> allPermissionCodes() {
        return List.copyOf(Permission.allCodes());
    }

    private Role require(String id) {
        return roleRepository.findById(id).orElseThrow(() -> BusinessException.notFound("角色不存在: " + id));
    }

    private Set<String> validatePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw BusinessException.badRequest("至少选择一个权限");
        }
        Set<String> result = new LinkedHashSet<>();
        for (String code : permissions) {
            String normalized = code == null ? "" : code.trim();
            if (!Permission.exists(normalized)) {
                throw BusinessException.badRequest("未知权限编码: " + code);
            }
            result.add(normalized);
        }
        // 按枚举顺序输出，保证前端展示稳定
        return Permission.allCodes().stream().filter(result::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 角色 id -> 使用该角色的人数。 */
    Map<String, Long> userCountsByRole() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (User user : userRepository.findAll()) {
            if (user.getRoleIds() == null) {
                continue;
            }
            user.getRoleIds().stream().filter(Objects::nonNull).distinct()
                    .forEach(roleId -> counts.merge(roleId, 1L, Long::sum));
        }
        return counts;
    }

    private RoleView toView(Role role, long userCount) {
        return new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                role.getPermissions() == null ? List.of() : List.copyOf(role.getPermissions()),
                role.isBuiltIn(), userCount, role.getCreatedAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
