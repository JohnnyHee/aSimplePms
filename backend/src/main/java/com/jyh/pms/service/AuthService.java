package com.jyh.pms.service;

import com.jyh.pms.core.config.PmsProperties;
import com.jyh.pms.core.error.BusinessException;
import com.jyh.pms.core.error.ErrorCode;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.domain.User;
import com.jyh.pms.domain.UserStatus;
import com.jyh.pms.security.AuthException;
import com.jyh.pms.security.AuthUser;
import com.jyh.pms.security.JwtTokenProvider;
import com.jyh.pms.security.RefreshSession;
import com.jyh.pms.security.TokenStore;
import com.jyh.pms.security.UserAccount;
import com.jyh.pms.storage.UserRepository;
import com.jyh.pms.web.MenuCatalog;
import com.jyh.pms.web.dto.CurrentUserView;
import com.jyh.pms.web.dto.LoginView;
import com.jyh.pms.web.dto.UserView;
import com.jyh.pms.web.dto.req.PasswordChangeRequest;
import com.jyh.pms.web.dto.req.ProfileUpdateRequest;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 认证服务：登录（含失败锁定）、刷新令牌轮换、登出（访问令牌拉黑 + 刷新令牌吊销）、修改本人密码。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserAccountAdapter accountAdapter;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final PmsProperties properties;
    private final UserService userService;

    public AuthService(UserRepository userRepository, UserAccountAdapter accountAdapter,
                       JwtTokenProvider tokenProvider, TokenStore tokenStore, PasswordEncoder passwordEncoder,
                       PmsProperties properties, UserService userService) {
        this.userRepository = userRepository;
        this.accountAdapter = accountAdapter;
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.userService = userService;
    }

    /**
     * 登录。连续失败达到 {@code pms.security.max-login-failures} 后锁定账号
     * {@code pms.security.lock-duration}。
     */
    public LoginView login(String username, String password, HttpServletRequest request) {
        String loginName = username == null ? "" : username.trim();
        User user = userRepository.findByUsername(loginName)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "账号已锁定，请于 " + user.getLockedUntil() + " 后重试");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            int maxFailures = properties.getSecurity().getMaxLoginFailures();
            if (failures >= maxFailures) {
                user.setLockedUntil(now.plus(properties.getSecurity().getLockDuration()));
                log.warn("账号 {} 连续登录失败 {} 次，已锁定至 {}", loginName, failures, user.getLockedUntil());
            }
            user.setUpdatedAt(now);
            userRepository.save(user);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }

        UserAccount account = accountAdapter.findByUsername(loginName)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        tokenStore.clearLoginFailures(loginName);
        return issue(account, request);
    }

    /** 用刷新令牌换取新的令牌对（旧刷新令牌立即失效 —— 轮换策略）。 */
    public LoginView refresh(String refreshToken) {
        Claims claims = tokenProvider.parse(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new AuthException(ErrorCode.TOKEN_INVALID, "令牌类型不正确");
        }
        String jti = claims.getId();
        RefreshSession session = tokenStore.findRefreshToken(jti)
                .orElseThrow(() -> new AuthException(ErrorCode.TOKEN_INVALID, "刷新令牌已失效，请重新登录"));
        tokenStore.revokeRefreshToken(jti);
        UserAccount account = accountAdapter.findByUid(session.uid())
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHENTICATED, "账号不存在"));
        if (!account.enabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        User user = userRepository.findById(account.uid())
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHENTICATED, "账号不存在"));
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        return issue(account, null);
    }

    /** 登出：吊销刷新令牌并把当前访问令牌拉黑（若可解析）。 */
    public void logout(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            Claims claims = tokenProvider.parseIgnoringExpiration(refreshToken);
            if (claims != null) {
                tokenStore.revokeRefreshToken(claims.getId());
            }
        }
        if (accessToken != null && !accessToken.isBlank()) {
            Claims claims = tokenProvider.parseIgnoringExpiration(accessToken);
            if (claims != null) {
                Duration ttl = Duration.between(Instant.now(), claims.getExpiration().toInstant());
                tokenStore.blacklistAccessToken(claims.getId(), ttl.isNegative() ? Duration.ZERO : ttl);
            }
        }
    }

    /** 从请求头取访问令牌原文。 */
    public static String accessTokenOf(HttpServletRequest request) {
        String header = request == null ? null : request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    /** 修改本人密码：校验原密码，成功后所有会话失效（返回新的令牌对让当前端继续使用）。 */
    public void changePassword(String uid, PasswordChangeRequest request) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> BusinessException.notFound("人员不存在: " + uid));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        tokenStore.revokeAllForUser(uid);
        log.info("用户 {} 修改了密码，已吊销其全部会话", user.getUsername());
    }

    /** 修改本人资料。 */
    public UserView updateProfile(String uid, ProfileUpdateRequest request) {
        return userService.updateProfile(uid, request);
    }

    /** 当前登录用户信息（含角色、权限）。 */
    public CurrentUserView currentUser() {
        AuthUser principal = currentPrincipal();
        return new CurrentUserView(principal.uid(), principal.username(),
                displayName(principal.uid()), principal.roles(), principal.permissions());
    }

    /** 当前用户可见菜单，{@code GET /api/auth/permissions} 使用。 */
    public List<MenuCatalog.MenuItem> menus() {
        AuthUser principal = currentPrincipal();
        return MenuCatalog.all().stream()
                .filter(item -> item.permission() == null || principal.permissions().contains(item.permission()))
                .toList();
    }

    /** 当前用户权限编码，{@code GET /api/auth/permissions} 使用。 */
    public List<String> permissions() {
        return currentPrincipal().permissions();
    }

    /** 当前用户角色编码。 */
    public List<String> roles() {
        return currentPrincipal().roles();
    }

    /** 当前登录用户 id。 */
    public String currentUid() {
        return currentPrincipal().uid();
    }

    private AuthUser currentPrincipal() {
        AuthUser principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new AuthException(ErrorCode.UNAUTHENTICATED, "未登录");
        }
        return principal;
    }

    private String displayName(String uid) {
        return userRepository.findById(uid)
                .map(user -> user.getName() != null && !user.getName().isBlank() ? user.getName() : user.getUsername())
                .orElse(SecurityUtils.currentUsername());
    }

    /** 签发令牌对并登记刷新会话。 */
    private LoginView issue(UserAccount account, HttpServletRequest request) {
        JwtTokenProvider.IssuedToken access = tokenProvider.issueAccessToken(
                account.uid(), account.username(), account.roles(), account.permissions());
        JwtTokenProvider.IssuedToken refresh = tokenProvider.issueRefreshToken(account.uid(), account.username());
        tokenStore.saveRefreshToken(
                new RefreshSession(refresh.jti(), account.uid(), account.username(), refresh.issuedAt(), refresh.expiresAt()),
                tokenProvider.refreshTtl());
        UserView view = userService.get(account.uid());
        long expiresIn = Duration.between(Instant.now(), access.expiresAt()).toSeconds();
        log.info("用户 {} 登录成功，来源 ip={}", account.username(),
                request == null ? "-" : request.getRemoteAddr());
        return LoginView.bearer(access.token(), refresh.token(), expiresIn, view);
    }
}
