package com.jyh.pms.web;

import com.jyh.pms.core.audit.Audited;
import com.jyh.pms.core.web.ApiResponse;
import com.jyh.pms.core.web.PageQuery;
import com.jyh.pms.core.web.PageResult;
import com.jyh.pms.core.web.SecurityUtils;
import com.jyh.pms.service.PositionService;
import com.jyh.pms.web.dto.PositionView;
import com.jyh.pms.web.dto.req.PositionRequest;
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
 * 职位管理接口。
 */
@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    /** 分页查询职位。 */
    @GetMapping
    @PreAuthorize("hasAuthority('position:view')")
    public ApiResponse<PageResult<PositionView>> page(PageQuery pageQuery) {
        return ApiResponse.ok(SecurityUtils.toPage(positionService.page(pageQuery), pageQuery));
    }

    /** 职位下拉选项。 */
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('position:view','user:view','user:create','user:update','salary:view','salary:create','salary:update')")
    public ApiResponse<List<PositionView>> options() {
        return ApiResponse.ok(positionService.options());
    }

    /** 职位详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('position:view')")
    public ApiResponse<PositionView> get(@PathVariable String id) {
        return ApiResponse.ok(positionService.get(id));
    }

    /** 新增职位。 */
    @PostMapping
    @PreAuthorize("hasAuthority('position:create')")
    @Audited(action = "新增职位")
    public ApiResponse<PositionView> create(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.ok(positionService.create(request));
    }

    /** 修改职位。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('position:update')")
    @Audited(action = "修改职位")
    public ApiResponse<PositionView> update(@PathVariable String id, @Valid @RequestBody PositionRequest request) {
        return ApiResponse.ok(positionService.update(id, request));
    }

    /** 删除职位。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('position:delete')")
    @Audited(action = "删除职位")
    public ApiResponse<Void> delete(@PathVariable String id) {
        positionService.delete(id);
        return ApiResponse.ok();
    }
}
