package com.jyh.pms.service;

import com.jyh.pms.domain.Permission;
import com.jyh.pms.domain.Role;
import com.jyh.pms.domain.User;
import com.jyh.pms.domain.UserStatus;
import com.jyh.pms.security.UserAccount;
import com.jyh.pms.security.UserAccountPort;
import com.jyh.pms.storage.RoleRepository;
import com.jyh.pms.storage.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@link UserAccountPort} 的适配器：把人员仓储 + 角色仓储聚合成认证所需的账号视图。
 * 角色/权限解析集中在这里，登录、刷新令牌与过滤器共用同一套口径。
 */
@Service
public class UserAccountAdapter implements UserAccountPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserAccountAdapter(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.trim()).map(this::toAccount);
    }

    @Override
    public Optional<UserAccount> findByUid(String uid) {
        return userRepository.findById(uid).map(this::toAccount);
    }

    /** 解析人员当前的角色编码与权限编码。 */
    public Authorization resolveAuthorization(User user) {
        List<Role> roles = user.getRoleIds() == null || user.getRoleIds().isEmpty()
                ? List.of()
                : roleRepository.findAllById(user.getRoleIds());
        List<String> roleCodes = roles.stream().map(Role::getCode).filter(Objects::nonNull).toList();
        Set<String> permissions = new LinkedHashSet<>();
        roles.stream()
                .map(Role::getPermissions)
                .filter(Objects::nonNull)
                .forEach(permissions::addAll);
        // 按权限枚举顺序输出，保证前端按钮顺序稳定
        List<String> ordered = Permission.allCodes().stream().filter(permissions::contains).toList();
        return new Authorization(roleCodes, ordered);
    }

    private UserAccount toAccount(User user) {
        Authorization authorization = resolveAuthorization(user);
        boolean enabled = user.getStatus() == null || user.getStatus() == UserStatus.ENABLED;
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        String displayName = user.getName() != null && !user.getName().isBlank()
                ? user.getName()
                : (user.getNickname() != null ? user.getNickname() : user.getUsername());
        return new UserAccount(user.getUid(), user.getUsername(), displayName, user.getPassword(),
                enabled, locked, authorization.roles(), authorization.permissions());
    }

    /**
     * 授权信息。
     *
     * @param roles       角色编码
     * @param permissions 权限编码（按枚举顺序）
     */
    public record Authorization(List<String> roles, List<String> permissions) {

        /** 便捷方法：以 {@code code -> code} 形式返回权限集合。 */
        public Set<String> permissionSet() {
            return permissions == null ? Set.of() : Set.copyOf(permissions);
        }
    }
}
