package com.jyh.pms.storage;

import com.jyh.pms.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * 人员（User）领域查询仓储。通用 CRUD 由 {@link DocumentRepository} 提供，
 * 这里只声明与人员业务相关的查询方法。实现类按 {@code pms.storage} 配置互斥生效。
 */
public interface UserRepository extends DocumentRepository<User> {

    /** 按登录名查询（忽略大小写）。 */
    Optional<User> findByUsername(String username);

    /** 登录名是否已存在（忽略大小写）。 */
    boolean existsByUsername(String username);

    /** 查询拥有指定角色 id 的全部人员。 */
    List<User> findByRoleId(String roleId);

    /** 统计关联到指定职位的人员数量。 */
    long countByPositionId(String positionId);
}
