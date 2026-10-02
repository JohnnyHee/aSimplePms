package com.jyh.pms.web.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增角色请求。
 */
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色编码需为大写字母开头、2-32 位大写字母/数字/下划线")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称过长")
    private String name;

    @Size(max = 255, message = "描述过长")
    private String description;

    @NotEmpty(message = "至少选择一个权限")
    private List<String> permissions;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
