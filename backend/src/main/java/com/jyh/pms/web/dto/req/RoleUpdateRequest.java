package com.jyh.pms.web.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 修改角色请求（角色编码不可改，内置角色的权限不可改）。
 */
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称过长")
    private String name;

    @Size(max = 255, message = "描述过长")
    private String description;

    @NotEmpty(message = "至少选择一个权限")
    private List<String> permissions;

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
