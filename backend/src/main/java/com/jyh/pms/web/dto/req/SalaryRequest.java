package com.jyh.pms.web.dto.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新增 / 修改薪资记录请求。
 */
public class SalaryRequest {

    @NotBlank(message = "人员不能为空")
    private String uid;

    private String positionId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", message = "金额不能为负")
    @Digits(integer = 10, fraction = 2, message = "金额最多 10 位整数、2 位小数")
    private BigDecimal amount;

    @NotBlank(message = "薪资类型不能为空")
    @Pattern(regexp = "MONTHLY|BONUS|ALLOWANCE|DEDUCTION", message = "薪资类型不正确")
    private String payType;

    @Pattern(regexp = "^$|ACTIVE|CLOSED", message = "状态只能是 ACTIVE 或 CLOSED")
    private String status;

    /** 生效日期，格式 yyyy-MM-dd。 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "生效日期格式应为 yyyy-MM-dd")
    private String effectiveFrom;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "失效日期格式应为 yyyy-MM-dd")
    private String effectiveTo;

    @Size(max = 255, message = "备注过长")
    private String remark;

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

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(String effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(String effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
