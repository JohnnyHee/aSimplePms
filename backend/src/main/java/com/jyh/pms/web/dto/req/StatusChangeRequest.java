package com.jyh.pms.web.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 修改人员状态请求（启用 / 禁用）。
 */
public class StatusChangeRequest {

    @NotBlank(message = "status 不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "status 只能是 ENABLED 或 DISABLED")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
