package com.jyh.pms.web.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改职位请求。
 */
public class PositionRequest {

    @NotBlank(message = "职位编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "职位编码需为大写字母开头、2-32 位大写字母/数字/下划线")
    private String code;

    @NotBlank(message = "职位名称不能为空")
    @Size(max = 32, message = "职位名称过长")
    private String name;

    @Size(max = 16, message = "职级过长")
    private String level;

    @Size(max = 255, message = "描述过长")
    private String description;

    @Min(value = 0, message = "排序值不能为负")
    private Integer sortOrder;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
