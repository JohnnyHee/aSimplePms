package com.jyh.pms.web;

import com.jyh.pms.core.audit.Audited;
import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.PageResult;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.service.UserService;
import com.jyh.pms.web.dto.UserView;
import com.jyh.pms.web.dto.req.PasswordResetRequest;
import com.jyh.pms.web.dto.req.StatusChangeRequest;
import com.jyh.pms.web.dto.req.UserCreateRequest;
import com.jyh.pms.web.dto.req.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 人员管理接口。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 分页查询人员。 */
    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<PageResult<UserView>> page(PageQuery pageQuery, UserQuery query) {
        return ApiResponse.ok(SecurityUtils.toPage(userService.page(pageQuery, query), pageQuery));
    }

    /** 全部部门（搜索下拉用）。 */
    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<List<String>> departments() {
        return ApiResponse.ok(userService.departments());
    }

    /** 人员详情。 */
    @GetMapping("/{uid}")
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<UserView> get(@PathVariable String uid) {
        return ApiResponse.ok(userService.get(uid));
    }

    /** 新增人员。 */
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    @Audited(action = "新增人员")
    public ApiResponse<UserView> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    /** 修改人员。 */
    @PutMapping("/{uid}")
    @PreAuthorize("hasAuthority('user:update')")
    @Audited(action = "修改人员")
    public ApiResponse<UserView> update(@PathVariable String uid, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(uid, request));
    }

    /** 删除人员。 */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasAuthority('user:delete')")
    @Audited(action = "删除人员")
    public ApiResponse<Void> delete(@PathVariable String uid) {
        userService.delete(uid);
        return ApiResponse.ok();
    }

    /** 启用/禁用人员。 */
    @PatchMapping("/{uid}/status")
    @PreAuthorize("hasAuthority('user:update')")
    @Audited(action = "修改人员状态")
    public ApiResponse<UserView> changeStatus(@PathVariable String uid, @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(userService.changeStatus(uid, request.getStatus()));
    }

    /** 重置他人密码。 */
    @PutMapping("/{uid}/password")
    @PreAuthorize("hasAuthority('user:reset-password')")
    @Audited(action = "重置人员密码", recordDetail = false)
    public ApiResponse<Void> resetPassword(@PathVariable String uid, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(uid, request);
        return ApiResponse.ok();
    }

    /** 供前端校验登录名是否已被占用。 */
    @GetMapping("/check-username")
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        return ApiResponse.ok(userService.findByUsername(username).isEmpty());
    }
}
