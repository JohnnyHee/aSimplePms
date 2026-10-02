package com.jyh.pms.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色实体。对应旧项目的 {@code com.jyh.main.modle.Role}（旧字段仅 role_id/role_name），
 * 新增权限列表与内置角色标记，从而替代写死在 Shiro 配置里的角色判断。
 */
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    /** 角色编码，唯一，例如 ADMIN / MANAGER / USER。 */
    @Indexed(unique = true)
    private String code;

    /** 角色名称。 */
    private String name;

    private String description;

    /** 权限编码列表，取自 {@link Permission#code()}。 */
    private List<String> permissions = new ArrayList<>();

    /** 内置角色不允许删除，权限也不允许改。 */
    private boolean builtIn;

    private Instant createdAt;

    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
        this.permissions = permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
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
