package com.jyh.pms.storage;

import com.jyh.pms.domain.Role;

import java.util.List;
import java.util.Optional;

/**
 * 角色（Role）领域查询仓储。通用 CRUD 由 {@link DocumentRepository} 提供，
 * 这里只声明与角色业务相关的查询方法。
 */
public interface RoleRepository extends DocumentRepository<Role> {

    /** 按角色编码查询。 */
    Optional<Role> findByCode(String code);

    /** 角色编码是否已存在。 */
    boolean existsByCode(String code);

    /** 按角色编码批量查询。 */
    List<Role> findByCodeIn(List<String> codes);
}
