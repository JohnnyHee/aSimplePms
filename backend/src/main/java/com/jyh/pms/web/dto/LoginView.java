package com.jyh.pms.web.dto;

/**
 * 登录 / 刷新令牌成功后返回的令牌信封。
 */
public record LoginView(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserView user) {

    public static LoginView bearer(String accessToken, String refreshToken, long expiresIn, UserView user) {
        return new LoginView(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
