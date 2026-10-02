package com.jyh.pms.web;

import com.jyh.pms.core.audit.Audited;
import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.PageResult;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.domain.Salary;
import com.jyh.pms.service.SalaryService;
import com.jyh.pms.web.dto.SalaryView;
import com.jyh.pms.web.dto.req.SalaryRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 薪资管理接口。
 */
@RestController
@RequestMapping("/api/salaries")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    /** 分页查询薪资记录。 */
    @GetMapping
    @PreAuthorize("hasAuthority('salary:view')")
    public ApiResponse<PageResult<SalaryView>> page(PageQuery pageQuery, SalaryQuery query) {
        return ApiResponse.ok(SecurityUtils.toPage(salaryService.page(pageQuery, query), pageQuery));
    }

    /** 某人的调薪历史。 */
    @GetMapping("/history/{uid}")
    @PreAuthorize("hasAuthority('salary:view')")
    public ApiResponse<List<Salary>> history(@PathVariable String uid) {
        return ApiResponse.ok(salaryService.history(uid));
    }

    /** 新增薪资记录。 */
    @PostMapping
    @PreAuthorize("hasAuthority('salary:create')")
    @Audited(action = "新增薪资记录")
    public ApiResponse<SalaryView> create(@Valid @RequestBody SalaryRequest request) {
        return ApiResponse.ok(salaryService.create(request));
    }

    /** 修改薪资记录。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('salary:update')")
    @Audited(action = "修改薪资记录")
    public ApiResponse<SalaryView> update(@PathVariable String id, @Valid @RequestBody SalaryRequest request) {
        return ApiResponse.ok(salaryService.update(id, request));
    }

    /** 删除薪资记录。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('salary:delete')")
    @Audited(action = "删除薪资记录")
    public ApiResponse<Void> delete(@PathVariable String id) {
        salaryService.delete(id);
        return ApiResponse.ok();
    }
}
