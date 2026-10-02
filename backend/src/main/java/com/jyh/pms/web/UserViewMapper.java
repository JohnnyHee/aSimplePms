package com.jyh.pms.web;

import com.jyh.pms.domain.Position;
import com.jyh.pms.domain.Role;
import com.jyh.pms.domain.User;
import com.jyh.pms.domain.UserStatus;
import com.jyh.pms.storage.PositionRepository;
import com.jyh.pms.storage.RoleRepository;
import com.jyh.pms.web.dto.UserView;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 人员实体 -> 视图对象转换。集中处理角色名、职位名的补全，避免 N+1：调用方可先取好映射表。
 */
@Component
public class UserViewMapper {

    /**
     * 把人员实体转换为视图对象。
     *
     * @param user            人员实体
     * @param roleNamesById   角色 id -> 角色名映射（可为空）
     * @param positionNames   职位 id -> 职位名映射（可为空）
     */
    public UserView toView(User user, Map<String, String> roleNamesById, Map<String, String> positionNames) {
        List<String> roleIds = user.getRoleIds() == null ? List.of() : List.copyOf(user.getRoleIds());
        List<String> roleNames = roleIds.stream()
                .map(id -> roleNamesById == null ? null : roleNamesById.get(id))
                .filter(Objects::nonNull)
                .toList();
        UserStatus status = user.getStatus() == null ? UserStatus.ENABLED : user.getStatus();
        String positionId = user.getPositionId();
        String positionName = positionId == null || positionNames == null ? null : positionNames.get(positionId);
        return new UserView(
                user.getUid(),
                user.getUsername(),
                user.getName(),
                user.getNickname(),
                user.getDepartment(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getRemark(),
                user.getAge(),
                status.name(),
                status.label(),
                roleIds,
                roleNames,
                positionId,
                positionName,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    /** 组装"角色 id -> 角色名"映射表。 */
    public static Map<String, String> roleNameMap(List<Role> roles) {
        Map<String, String> map = new LinkedHashMap<>();
        if (roles != null) {
            roles.forEach(role -> map.put(role.getId(), role.getName()));
        }
        return map;
    }

    /** 组装"职位 id -> 职位名"映射表。 */
    public static Map<String, String> positionNameMap(List<Position> positions) {
        Map<String, String> map = new LinkedHashMap<>();
        if (positions != null) {
            positions.forEach(position -> map.put(position.getId(), position.getName()));
        }
        return map;
    }

    /** 便捷方法：直接查库补全（单条转换时使用）。 */
    public UserView toView(User user, RoleRepository roleRepository, PositionRepository positionRepository) {
        Map<String, String> roleNames = new LinkedHashMap<>();
        if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            roleNames.putAll(roleNameMap(roleRepository.findAllById(user.getRoleIds())));
        }
        Map<String, String> positionNames = new LinkedHashMap<>();
        if (user.getPositionId() != null) {
            positionRepository.findById(user.getPositionId())
                    .ifPresent(position -> positionNames.put(position.getId(), position.getName()));
        }
        return toView(user, roleNames, positionNames);
    }
}
