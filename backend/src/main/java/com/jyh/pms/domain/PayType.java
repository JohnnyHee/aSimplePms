package com.jyh.pms.domain;

/**
 * 薪资类型。
 */
public enum PayType {

    MONTHLY("基本月薪"),
    BONUS("绩效奖金"),
    ALLOWANCE("津贴补助"),
    DEDUCTION("扣款");

    private final String label;

    PayType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
