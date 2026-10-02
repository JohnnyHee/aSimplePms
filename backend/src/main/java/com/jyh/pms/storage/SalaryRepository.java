package com.jyh.pms.storage;

import com.jyh.pms.domain.Salary;

import java.util.List;
import java.util.Optional;

/**
 * 薪资（Salary）领域查询仓储。通用 CRUD 由 {@link DocumentRepository} 提供，
 * 这里只声明与薪资业务相关的查询方法（调薪历史、当前生效薪资、统计）。
 */
public interface SalaryRepository extends DocumentRepository<Salary> {

    /** 查询某人的全部调薪记录，按生效日期倒序（最新在前）。 */
    List<Salary> findByUidOrderByEffectiveFromDesc(String uid);

    /** 查询某人当前生效的薪资（status = ACTIVE）。 */
    Optional<Salary> findActiveByUid(String uid);

    /** 统计某人的薪资记录数。 */
    long countByUid(String uid);

    /** 统计关联到指定职位的薪资记录数。 */
    long countByPositionId(String positionId);
}
