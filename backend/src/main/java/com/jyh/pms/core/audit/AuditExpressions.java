package com.jyh.pms.core.audit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 审计日志操作人的显式覆盖入口。
 *
 * <p>常规情况下 {@link AuditLogInterceptor} 能从登录态、请求属性或令牌声明里推断出操作人。
 * 但有两类请求推断不出来：</p>
 * <ul>
 *   <li><b>登录</b>：请求结束时还没有令牌、SecurityContext 也已随请求清理，
 *       只能得到 {@code anonymous}，而审计里恰恰最需要知道是谁登录的；</li>
 *   <li><b>改密码 / 重置密码</b>：操作人既不是被改的人，也不一定体现在 URL 里。</li>
 * </ul>
 *
 * <p>这些场景由控制器在处理过程中调用 {@link #setUsername} 主动声明操作人
 * （无论是成功还是失败路径，只要在返回前调用即可，因为审计是在
 * {@code afterCompletion} 阶段落库的）。</p>
 */
public final class AuditExpressions {

    /** 请求属性名，存放本次请求的审计操作人。 */
    public static final String USERNAME_ATTRIBUTE = "pms.audit.username";

    private AuditExpressions() {
    }

    /** 主动指定本次请求的审计操作人，空白值会被忽略。 */
    public static void setUsername(HttpServletRequest request, String username) {
        if (username != null && !username.isBlank()) {
            request.setAttribute(USERNAME_ATTRIBUTE, username.trim());
        }
    }

    /** 读取显式指定的操作人，未指定时返回 {@code null} 由调用方回退到常规解析。 */
    static String explicitUsername(HttpServletRequest request) {
        Object value = request.getAttribute(USERNAME_ATTRIBUTE);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return null;
    }
}
