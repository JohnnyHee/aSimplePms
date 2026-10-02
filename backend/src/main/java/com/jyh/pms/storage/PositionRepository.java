package com.jyh.pms.storage;

import com.jyh.pms.domain.Position;

import java.util.List;
import java.util.Optional;

/**
 * 职位（Position）领域查询仓储。通用 CRUD 由 {@link DocumentRepository} 提供，
 * 这里只声明与职位业务相关的查询方法。
 */
public interface PositionRepository extends DocumentRepository<Position> {

    /** 按职位编码查询。 */
    Optional<Position> findByCode(String code);

    /** 职位编码是否已存在。 */
    boolean existsByCode(String code);

    /** 查询全部职位，按 sortOrder 升序（下拉选项等场景）。 */
    List<Position> findAllOrderBySortOrder();
}
