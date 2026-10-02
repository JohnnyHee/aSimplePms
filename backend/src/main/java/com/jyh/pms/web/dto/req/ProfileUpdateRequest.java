package com.jyh.pms.web.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改本人资料请求（不含登录名、密码、角色、状态等管理员字段）。
 */
public class ProfileUpdateRequest {

    @Size(max = 32, message = "姓名过长")
    private String name;

    @Size(max = 32, message = "昵称过长")
    private String nickname;

    @Size(max = 32, message = "部门名称过长")
    private String department;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱过长")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Min(value = 16, message = "年龄不能小于 16")
    @Max(value = 100, message = "年龄不能大于 100")
    private Integer age;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Size(max = 255, message = "备注过长")
    private String remark;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
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
}
