package com.jyh.pms.domain;

/**
 * 薪资记录状态。
 */
public enum SalaryStatus {

    /** 当前生效。 */
    ACTIVE("生效中"),
    /** 已归档（被新的调薪记录取代或人员离职）。 */
    CLOSED("已归档");

    private final String label;

    SalaryStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
