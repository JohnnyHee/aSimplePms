package com.jyh.pms.core.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入审计日志的接口方法。
 * 由 {@link AuditLogInterceptor} 统一采集请求信息、耗时与执行结果。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** 业务动作名称，如「新增人员」「删除角色」。 */
    String action();

    /** 是否记录请求明细（不影响业务数据，仅用于审计展示）。 */
    boolean recordDetail() default true;
}
