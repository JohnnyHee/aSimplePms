package com.jyh.pms.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 操作审计日志：记录每一次写操作（以及登录/登出），旧项目完全没有该能力。
 */
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    /** 操作人登录名，匿名请求为 anonymous。 */
    @Indexed
    private String username;

    /** 操作人 uid。 */
    private String uid;

    /** 动作，例如 user:create。 */
    private String action;

    /** HTTP 方法。 */
    private String method;

    /** 请求路径。 */
    private String path;

    /** 客户端 IP。 */
    private String ip;

    /** 结果：SUCCESS / FAILURE。 */
    private String outcome;

    /** 失败原因或补充说明。 */
    private String detail;

    /** 耗时（毫秒）。 */
    private long elapsedMs;

    @Indexed
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
