package com.jyh.pms.domain;

/**
 * 账号状态。
 */
public enum UserStatus {

    /** 正常可用。 */
    ENABLED("正常"),
    /** 已禁用，无法登录。 */
    DISABLED("已禁用");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
