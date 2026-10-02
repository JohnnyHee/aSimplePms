package com.jyh.pms.domain;

/**
 * 内置角色常量。
 */
public enum RoleName {

    /** 系统管理员：拥有全部权限。 */
    ADMIN("系统管理员"),
    /** 部门主管：可维护人员与薪资。 */
    MANAGER("部门主管"),
    /** 普通员工：只读查看。 */
    USER("普通员工");

    private final String label;

    RoleName(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 角色编码，与枚举名一致。 */
    public String code() {
        return name();
    }

    public java.util.List<String> defaultPermissions() {
        return switch (this) {
            case ADMIN -> Permission.adminCodes();
            case MANAGER -> Permission.managerCodes();
            case USER -> Permission.employeeCodes();
        };
    }
}
