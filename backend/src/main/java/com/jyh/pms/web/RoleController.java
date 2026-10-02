package com.jyh.pms.web;

import com.jyh.pms.core.audit.Audited;
import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.PageResult;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.service.RoleService;
import com.jyh.pms.web.dto.PermissionGroupView;
import com.jyh.pms.web.dto.RoleView;
import com.jyh.pms.web.dto.req.RoleCreateRequest;
import com.jyh.pms.web.dto.req.RoleUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与权限接口。
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** 分页查询角色。 */
    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<PageResult<RoleView>> page(PageQuery pageQuery) {
        return ApiResponse.ok(SecurityUtils.toPage(roleService.page(pageQuery), pageQuery));
    }

    /** 角色下拉选项。 */
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('role:view','user:view','user:create','user:update')")
    public ApiResponse<List<RoleView>> options() {
        return ApiResponse.ok(roleService.options());
    }

    /** 权限目录（按分组）。 */
    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('role:view','role:create','role:update')")
    public ApiResponse<RolePermissionGroupsView> permissions() {
        return ApiResponse.ok(new RolePermissionGroupsView(roleService.permissionGroups()));
    }

    /** 角色详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<RoleView> get(@PathVariable String id) {
        return ApiResponse.ok(roleService.get(id));
    }

    /** 新增角色。 */
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    @Audited(action = "新增角色")
    public ApiResponse<RoleView> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    /** 修改角色。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    @Audited(action = "修改角色")
    public ApiResponse<RoleView> update(@PathVariable String id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.ok(roleService.update(id, request));
    }

    /** 删除角色。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    @Audited(action = "删除角色")
    public ApiResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }

    /** 权限分组响应：{@code {"groups":[...]}}。 */
    public record RolePermissionGroupsView(List<PermissionGroupView> groups) {
    }
}
