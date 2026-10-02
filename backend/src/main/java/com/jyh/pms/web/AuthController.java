package com.jyh.pms.web;

import com.jyh.pms.core.audit.AuditExpressions;
import com.jyh.pms.core.audit.Audited;
import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.service.AuthService;
import com.jyh.pms.web.dto.CurrentUserView;
import com.jyh.pms.web.dto.LoginView;
import com.jyh.pms.web.dto.UserPermissionsView;
import com.jyh.pms.web.dto.UserView;
import com.jyh.pms.web.dto.req.LoginRequest;
import com.jyh.pms.web.dto.req.PasswordChangeRequest;
import com.jyh.pms.web.dto.req.ProfileUpdateRequest;
import com.jyh.pms.web.dto.req.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口：登录、刷新、登出、当前用户、改密、改资料、权限目录。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录。 */
    @PostMapping("/login")
    @Audited(action = "用户登录")
    public ApiResponse<LoginView> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        // 登录成功时尚无令牌、SecurityContext 也随请求清理，审计拿不到操作人，
        // 这里显式声明提交的登录名（失败时同样记为尝试登录的账号）
        AuditExpressions.setUsername(servletRequest, request.getUsername());
        return ApiResponse.ok(authService.login(request.getUsername(), request.getPassword(), servletRequest));
    }

    /** 刷新令牌（轮换）。 */
    @PostMapping("/refresh")
    public ApiResponse<LoginView> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.getRefreshToken()));
    }

    /** 登出：吊销刷新令牌并拉黑当前访问令牌。 */
    @PostMapping("/logout")
    @Audited(action = "用户登出")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                    HttpServletRequest servletRequest) {
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.logout(refreshToken, AuthService.accessTokenOf(servletRequest));
        return ApiResponse.ok();
    }

    /** 当前登录用户。 */
    @GetMapping("/me")
    public ApiResponse<CurrentUserView> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    /** 修改本人密码。 */
    @PutMapping("/password")
    @Audited(action = "修改密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(SecurityUtils.currentUid(), request);
        return ApiResponse.ok();
    }

    /** 修改本人资料。 */
    @PutMapping("/profile")
    @Audited(action = "修改个人资料")
    public ApiResponse<UserView> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(authService.updateProfile(SecurityUtils.currentUid(), request));
    }

    /** 当前用户的权限与可见菜单。 */
    @GetMapping("/permissions")
    public ApiResponse<UserPermissionsView> permissions() {
        return ApiResponse.ok(new UserPermissionsView(authService.permissions(), authService.roles(), authService.menus()));
    }
}
