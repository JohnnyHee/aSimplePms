package com.jyh.pms.core.config;

import com.jyh.pms.domain.Permission;
import com.jyh.pms.domain.Position;
import com.jyh.pms.domain.Role;
import com.jyh.pms.domain.RoleName;
import com.jyh.pms.domain.User;
import com.jyh.pms.domain.UserStatus;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.RoleRepository;
import com.jyh.pms.storage.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 启动数据初始化：确保三个内置角色（系统管理员/部门主管/普通员工）、
 * 示例职位以及初始管理员账号存在，可重复执行（幂等）。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PmsProperties properties;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PmsProperties properties, RoleRepository roleRepository, UserRepository userRepository,
                           PositionRepository positionRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getInit().isEnabled()) {
            log.info("跳过初始化数据（pms.init.enabled=false）");
            return;
        }
        String adminRoleId = ensureRole(RoleName.ADMIN, List.copyOf(Permission.allCodes()));
        ensureRole(RoleName.MANAGER, RoleName.MANAGER.defaultPermissions());
        String userRoleId = ensureRole(RoleName.USER, RoleName.USER.defaultPermissions());
        String positionId = ensurePosition("P5", "初级工程师", "P5", 10);
        ensurePosition("P6", "高级工程师", "P6", 20);
        ensurePosition("M2", "部门经理", "M2", 30);
        ensureAdmin(adminRoleId, userRoleId, positionId);
        log.info("初始化数据检查完成：角色 {} 个、职位 {} 个、人员 {} 名",
                roleRepository.count(), positionRepository.count(), userRepository.count());
    }

    private String ensureRole(RoleName roleName, List<String> permissions) {
        return roleRepository.findByCode(roleName.code()).map(role -> {
            if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
                role.setPermissions(permissions);
                role.setUpdatedAt(Instant.now());
                roleRepository.save(role);
            }
            return role.getId();
        }).orElseGet(() -> {
            Instant now = Instant.now();
            Role role = new Role();
            role.setCode(roleName.code());
            role.setName(roleName.label());
            role.setDescription(roleName.label() + "（内置角色）");
            role.setPermissions(permissions);
            role.setBuiltIn(true);
            role.setCreatedAt(now);
            role.setUpdatedAt(now);
            Role saved = roleRepository.save(role);
            log.info("创建内置角色 {} - {}", saved.getCode(), saved.getName());
            return saved.getId();
        });
    }

    private String ensurePosition(String code, String name, String level, int sortOrder) {
        return positionRepository.findByCode(code).map(Position::getId).orElseGet(() -> {
            Instant now = Instant.now();
            Position position = new Position();
            position.setCode(code);
            position.setName(name);
            position.setLevel(level);
            position.setDescription(name);
            position.setSortOrder(sortOrder);
            position.setCreatedAt(now);
            position.setUpdatedAt(now);
            Position saved = positionRepository.save(position);
            log.info("创建职位 {} - {}", saved.getCode(), saved.getName());
            return saved.getId();
        });
    }

    private void ensureAdmin(String adminRoleId, String userRoleId, String positionId) {
        String username = properties.getInit().getAdminUsername();
        User existing = userRepository.findByUsername(username).orElse(null);
        if (existing != null) {
            if (existing.getRoleIds() == null || existing.getRoleIds().isEmpty()) {
                existing.setRoleIds(List.of(adminRoleId));
                existing.setUpdatedAt(Instant.now());
                userRepository.save(existing);
            }
            log.info("管理员账号已存在，跳过创建: {}", username);
            return;
        }
        Instant now = Instant.now();
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(properties.getInit().getAdminPassword()));
        admin.setName("系统管理员");
        admin.setNickname("admin");
        admin.setDepartment("信息中心");
        admin.setStatus(UserStatus.ENABLED);
        admin.setRoleIds(List.of(adminRoleId));
        admin.setPositionId(positionId);
        admin.setRemark("初始化账号，请登录后立即修改密码");
        admin.setFailedLoginCount(0);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userRepository.save(admin);
        // 保留一个普通员工角色 id 以便后续人工分配，避免 IDE 未使用告警
        log.info("创建初始管理员账号: {} / {}（角色 {}），默认员工角色 {}", username,
                properties.getInit().getAdminPassword(), adminRoleId, userRoleId);
    }
}
