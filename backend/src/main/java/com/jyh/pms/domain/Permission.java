package com.jyh.pms.domain;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 细粒度权限（菜单 + 操作）。角色通过勾选这些编码来授权，取代旧项目里写死在 ShiroConfig 的
 * {@code perms[admin]}。
 *
 * <p>编码规则：{@code <资源>:<操作>}，例如 {@code user:create}。</p>
 */
public enum Permission {

    USER_VIEW("user:view", "查看人员", "人员管理"),
    USER_CREATE("user:create", "新增人员", "人员管理"),
    USER_UPDATE("user:update", "编辑人员", "人员管理"),
    USER_DELETE("user:delete", "删除人员", "人员管理"),
    USER_RESET_PASSWORD("user:reset-password", "重置密码", "人员管理"),

    ROLE_VIEW("role:view", "查看角色", "角色权限"),
    ROLE_CREATE("role:create", "新增角色", "角色权限"),
    ROLE_UPDATE("role:update", "编辑角色", "角色权限"),
    ROLE_DELETE("role:delete", "删除角色", "角色权限"),

    POSITION_VIEW("position:view", "查看职位", "职位管理"),
    POSITION_CREATE("position:create", "新增职位", "职位管理"),
    POSITION_UPDATE("position:update", "编辑职位", "职位管理"),
    POSITION_DELETE("position:delete", "删除职位", "职位管理"),

    SALARY_VIEW("salary:view", "查看薪资", "薪资管理"),
    SALARY_CREATE("salary:create", "新增薪资", "薪资管理"),
    SALARY_UPDATE("salary:update", "编辑薪资", "薪资管理"),
    SALARY_DELETE("salary:delete", "删除薪资", "薪资管理"),

    AUDIT_VIEW("audit:view", "查看审计日志", "系统审计"),
    AUDIT_CLEAR("audit:clear", "清理审计日志", "系统审计");

    private final String code;
    private final String label;
    private final String group;

    Permission(String code, String label, String group) {
        this.code = code;
        this.label = label;
        this.group = group;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String group() {
        return group;
    }

    public static Optional<Permission> byCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(p -> p.code.equalsIgnoreCase(code.trim())).findFirst();
    }

    public static boolean exists(String code) {
        return byCode(code).isPresent();
    }

    public static Set<String> allCodes() {
        Set<String> codes = new LinkedHashSet<>();
        Arrays.stream(values()).forEach(p -> codes.add(p.code));
        return codes;
    }

    public static Set<String> codesOf(Permission... permissions) {
        Set<String> codes = new LinkedHashSet<>();
        Arrays.stream(permissions).forEach(p -> codes.add(p.code));
        return codes;
    }

    /** 普通员工：只能看人员、职位、薪资。 */
    public static List<String> employeeCodes() {
        return List.copyOf(codesOf(USER_VIEW, POSITION_VIEW, SALARY_VIEW));
    }

    /** 部门主管：在员工基础上可维护人员与薪资。 */
    public static List<String> managerCodes() {
        return List.copyOf(codesOf(USER_VIEW, USER_CREATE, USER_UPDATE,
                POSITION_VIEW, SALARY_VIEW, SALARY_CREATE, SALARY_UPDATE));
    }

    /** 系统管理员：全部权限。 */
    public static List<String> adminCodes() {
        return List.copyOf(allCodes());
    }
}
