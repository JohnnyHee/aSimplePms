package com.jyh.pms.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 人员（员工）实体。对应旧项目的 {@code com.jyh.main.modle.User}，字段做了现代化与规范化：
 * 旧的 {@code nikeName} -> {@code nickname}，新增 {@code department}、{@code email}、{@code phone}、
 * {@code status}、{@code roles}（多角色），密码改为 BCrypt 摘要存放，绝不返回给前端。
 */
@Document(collection = "users")
@CompoundIndex(name = "idx_user_name_dept", def = "{'name': 1, 'department': 1}")
public class User {

    /** 主键，同时作为工号使用（旧项目 u_id / uid）。 */
    @Id
    private String uid;

    /** 登录名，全局唯一。 */
    private String username;

    /** BCrypt 密码摘要（$2a$...）。 */
    private String password;

    /** 真实姓名。 */
    private String name;

    /** 昵称，取代旧字段 nikeName。 */
    private String nickname;

    private String department;

    private String email;

    private String phone;

    /** 头像地址。 */
    private String avatar;

    /** 简介 / 备注。 */
    private String remark;

    private Integer age;

    /** 账号状态。 */
    private UserStatus status = UserStatus.ENABLED;

    /** 所属角色 id 列表（旧的单 r_id 升级为多角色）。 */
    private List<String> roleIds = new ArrayList<>();

    /** 关联职位 id（旧 p_id / pos_id）。 */
    private String positionId;

    /** 最近一次登录时间。 */
    private Instant lastLoginAt;

    /** 连续登录失败次数，用于锁定。 */
    private int failedLoginCount;

    /** 锁定到期时间。 */
    private Instant lockedUntil;

    private Instant createdAt;

    private Instant updatedAt;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds == null ? new ArrayList<>() : new ArrayList<>(roleIds);
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
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
