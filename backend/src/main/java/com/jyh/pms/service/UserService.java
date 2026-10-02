package com.jyh.pms.service;

import com.jyh.pms.core.error.BusinessException;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.domain.Role;
import com.jyh.pms.domain.Salary;
import com.jyh.pms.domain.User;
import com.jyh.pms.domain.UserStatus;
import com.jyh.pms.security.TokenStore;
import com.jyh.pms.storage.PageData;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.RoleRepository;
import com.jyh.pms.storage.SalaryRepository;
import com.jyh.pms.storage.SortOption;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.web.UserQuery;
import com.jyh.pms.web.UserViewMapper;
import com.jyh.pms.web.dto.UserView;
import com.jyh.pms.web.dto.req.PasswordResetRequest;
import com.jyh.pms.web.dto.req.ProfileUpdateRequest;
import com.jyh.pms.web.dto.req.UserCreateRequest;
import com.jyh.pms.web.dto.req.UserUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 人员（员工）业务服务：分页检索、增删改、状态切换、密码重置。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PositionRepository positionRepository;
    private final SalaryRepository salaryRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final UserViewMapper viewMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PositionRepository positionRepository, SalaryRepository salaryRepository,
                       PasswordEncoder passwordEncoder, TokenStore tokenStore, UserViewMapper viewMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.positionRepository = positionRepository;
        this.salaryRepository = salaryRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.viewMapper = viewMapper;
    }

    /** 分页查询人员列表。 */
    public PageData<UserView> page(PageQuery pageQuery, UserQuery query) {
        Predicate<User> filter = filter(query);
        SortOption[] sorts = UserQuery.sortOptions(pageQuery);
        PageData<User> page = userRepository.page(null, filter, pageQuery, sorts);
        Map<String, String> roleNames = UserViewMapper.roleNameMap(roleRepository.findAll());
        Map<String, String> positionNames = UserViewMapper.positionNameMap(positionRepository.findAll());
        List<UserView> records = page.records().stream()
                .map(user -> viewMapper.toView(user, roleNames, positionNames))
                .toList();
        return new PageData<>(records, page.total());
    }

    /** 查询单个人员。 */
    public UserView get(String uid) {
        User user = require(uid);
        Map<String, String> roleNames = UserViewMapper.roleNameMap(roleRepository.findAllById(
                user.getRoleIds() == null ? List.of() : user.getRoleIds()));
        Map<String, String> positionNames = new LinkedHashMap<>();
        if (user.getPositionId() != null) {
            positionRepository.findById(user.getPositionId())
                    .ifPresent(position -> positionNames.put(position.getId(), position.getName()));
        }
        return viewMapper.toView(user, roleNames, positionNames);
    }

    /** 新增人员。 */
    public UserView create(UserCreateRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw BusinessException.conflict("登录名已存在: " + username);
        }
        validateRoleIds(request.getRoleIds());
        validatePositionId(request.getPositionId());

        Instant now = Instant.now();
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setNickname(trimToNull(request.getNickname()));
        user.setDepartment(trimToNull(request.getDepartment()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setAge(request.getAge());
        user.setRemark(trimToNull(request.getRemark()));
        user.setAvatar(trimToNull(request.getAvatar()));
        user.setRoleIds(normalizeIds(request.getRoleIds()));
        user.setPositionId(request.getPositionId());
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User saved = userRepository.save(user);
        log.info("新增人员 uid={}, username={}", saved.getUid(), saved.getUsername());
        return get(saved.getUid());
    }

    /** 修改人员（不含登录名与密码）。 */
    public UserView update(String uid, UserUpdateRequest request) {
        User user = require(uid);
        validateRoleIds(request.getRoleIds());
        validatePositionId(request.getPositionId());

        user.setName(request.getName().trim());
        user.setNickname(trimToNull(request.getNickname()));
        user.setDepartment(trimToNull(request.getDepartment()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setAge(request.getAge());
        user.setRemark(trimToNull(request.getRemark()));
        user.setAvatar(trimToNull(request.getAvatar()));
        if (request.getRoleIds() != null) {
            user.setRoleIds(normalizeIds(request.getRoleIds()));
        }
        if (request.getPositionId() != null) {
            user.setPositionId(request.getPositionId());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(parseStatus(request.getStatus()));
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return get(uid);
    }

    /** 修改人员状态（禁用会给该用户全部会话踢下线）。 */
    public UserView changeStatus(String uid, String status) {
        User user = require(uid);
        UserStatus target = parseStatus(status);
        if (uid.equals(SecurityUtils.currentUid()) && target == UserStatus.DISABLED) {
            throw BusinessException.business("不能禁用当前登录账号");
        }
        user.setStatus(target);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        if (target == UserStatus.DISABLED) {
            tokenStore.revokeAllForUser(uid);
            log.info("人员已禁用并强制下线 uid={}", uid);
        }
        return get(uid);
    }

    /** 删除人员（级联删除其薪资记录，禁止删除自己）。 */
    public void delete(String uid) {
        User user = require(uid);
        if (uid.equals(SecurityUtils.currentUid())) {
            throw BusinessException.business("不能删除当前登录账号");
        }
        List<Salary> salaries = salaryRepository.findByUidOrderByEffectiveFromDesc(uid);
        if (!salaries.isEmpty()) {
            salaryRepository.deleteAllById(salaries.stream().map(Salary::getId).toList());
        }
        userRepository.deleteById(uid);
        tokenStore.revokeAllForUser(uid);
        log.info("删除人员 uid={}, username={}, 级联删除薪资 {} 条", uid, user.getUsername(), salaries.size());
    }

    /** 管理员重置他人密码：重置后该用户全部会话失效。 */
    public void resetPassword(String uid, PasswordResetRequest request) {
        require(uid);
        User user = userRepository.findById(uid).orElseThrow(() -> BusinessException.notFound("人员不存在: " + uid));
        if (user.getUsername().equals(SecurityUtils.currentUsername())) {
            throw BusinessException.business("请使用个人中心的修改密码功能");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        tokenStore.revokeAllForUser(uid);
        log.info("管理员重置密码 uid={}", uid);
    }

    /**
     * 修改本人资料。
     */
    public UserView updateProfile(String uid, ProfileUpdateRequest request) {
        User user = require(uid);
        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getNickname() != null) {
            user.setNickname(trimToNull(request.getNickname()));
        }
        if (request.getDepartment() != null) {
            user.setDepartment(trimToNull(request.getDepartment()));
        }
        if (request.getEmail() != null) {
            user.setEmail(trimToNull(request.getEmail()));
        }
        if (request.getPhone() != null) {
            user.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(trimToNull(request.getAvatar()));
        }
        if (request.getRemark() != null) {
            user.setRemark(trimToNull(request.getRemark()));
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return get(uid);
    }

    /** 按登录名查询实体（内部使用）。 */
    public Optional<User> findByUsername(String username) {
        return username == null ? Optional.empty() : userRepository.findByUsername(username.trim());
    }

    /** 保存实体（认证流程记录登录时间等）。 */
    public void save(User user) {
        userRepository.save(user);
    }

    /** 供统计使用：全部部门名称（去重、非空）。 */
    public List<String> departments() {
        return userRepository.findAll().stream()
                .map(User::getDepartment)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(department -> !department.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private User require(String uid) {
        return userRepository.findById(uid).orElseThrow(() -> BusinessException.notFound("人员不存在: " + uid));
    }

    private void validateRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (String roleId : roleIds) {
            if (roleRepository.findById(roleId).isEmpty()) {
                throw BusinessException.badRequest("角色不存在: " + roleId);
            }
        }
    }

    private void validatePositionId(String positionId) {
        if (positionId == null || positionId.isBlank()) {
            return;
        }
        if (positionRepository.findById(positionId).isEmpty()) {
            throw BusinessException.badRequest("职位不存在: " + positionId);
        }
    }

    /** 组装筛选条件。 */
    static Predicate<User> filter(UserQuery query) {
        if (query == null) {
            return user -> true;
        }
        String keyword = query.getKeyword() == null ? null : query.getKeyword().trim().toLowerCase(Locale.ROOT);
        String roleId = trimToNull(query.getRoleId());
        String positionId = trimToNull(query.getPositionId());
        UserStatus status = query.getStatus() == null || query.getStatus().isBlank()
                ? null : parseStatus(query.getStatus());
        return user -> {
            if (status != null && (user.getStatus() == null ? UserStatus.ENABLED : user.getStatus()) != status) {
                return false;
            }
            if (roleId != null && (user.getRoleIds() == null || !user.getRoleIds().contains(roleId))) {
                return false;
            }
            if (positionId != null && !positionId.equals(user.getPositionId())) {
                return false;
            }
            if (keyword == null || keyword.isEmpty()) {
                return true;
            }
            return contains(user.getUsername(), keyword)
                    || contains(user.getName(), keyword)
                    || contains(user.getNickname(), keyword)
                    || contains(user.getDepartment(), keyword)
                    || contains(user.getEmail(), keyword)
                    || contains(user.getPhone(), keyword);
        };
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("状态只能是 ENABLED 或 DISABLED");
        }
    }

    private static List<String> normalizeIds(List<String> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        return ids.stream().filter(Objects::nonNull).map(String::trim)
                .filter(id -> !id.isEmpty()).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
