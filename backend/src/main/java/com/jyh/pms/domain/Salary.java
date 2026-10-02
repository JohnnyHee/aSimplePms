package com.jyh.pms.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 薪资记录。对应旧项目的 {@code com.jyh.main.modle.Salary}
 * （旧字段 u_id/p_id/salary/type/s_id），这里改为：
 * 主键 id、人员 uid、职位 positionId、金额 amount(BigDecimal)、类型 payType、
 * 生效区间 effectiveFrom/effectiveTo、状态 status，从而支持调薪历史。
 */
@Document(collection = "salaries")
@CompoundIndex(name = "idx_salary_user_status", def = "{'uid': 1, 'status': 1}")
public class Salary {

    @Id
    private String id;

    /** 人员 uid。 */
    @Indexed
    private String uid;

    /** 关联职位 id。 */
    private String positionId;

    /** 金额，单位元。 */
    private BigDecimal amount;

    private PayType payType = PayType.MONTHLY;

    private SalaryStatus status = SalaryStatus.ACTIVE;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String remark;

    /** 上次操作的登录名，便于追溯。 */
    private String operator;

    private Instant createdAt;

    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PayType getPayType() {
        return payType;
    }

    public void setPayType(PayType payType) {
        this.payType = payType;
    }

    public SalaryStatus getStatus() {
        return status;
    }

    public void setStatus(SalaryStatus status) {
        this.status = status;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
