package com.anes.schedule.controller;

import com.anes.schedule.common.ApiResponse;
import com.anes.schedule.common.AuthContext;
import com.anes.schedule.common.PageResult;
import com.anes.schedule.dto.RuleDtos.RuleQuery;
import com.anes.schedule.dto.RuleDtos.RuleResp;
import com.anes.schedule.dto.RuleDtos.RuleSaveReq;
import com.anes.schedule.dto.RuleDtos.VersionResp;
import com.anes.schedule.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "规则中心")
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @Operation(summary = "分页查询规则(分类/状态/关键字)")
    @GetMapping
    public ApiResponse<PageResult<RuleResp>> page(RuleQuery query) {
        return ApiResponse.ok(ruleService.page(query));
    }

    @Operation(summary = "版本历史(倒序)")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<VersionResp>> versions(@PathVariable Long id) {
        return ApiResponse.ok(ruleService.versions(id));
    }

    @Operation(summary = "payload 中文回读预览")
    @PostMapping("/preview")
    public ApiResponse<String> preview(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(ruleService.preview(body.get("payload")));
    }

    @Operation(summary = "新增规则(仅 ADMIN,初始 DRAFT)")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody @Valid RuleSaveReq req) {
        AuthContext.requireAdmin();
        ruleService.create(req);
        return ApiResponse.ok();
    }

    @Operation(summary = "修改规则(仅 ADMIN)")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid RuleSaveReq req) {
        AuthContext.requireAdmin();
        ruleService.update(id, req);
        return ApiResponse.ok();
    }

    @Operation(summary = "发布规则(仅 ADMIN,版本+1 并置 ACTIVE)")
    @PostMapping("/{id}/publish")
    public ApiResponse<Integer> publish(@PathVariable Long id) {
        Long userId = AuthContext.requireAdmin().userId();
        return ApiResponse.ok(ruleService.publish(id, userId));
    }

    @Operation(summary = "退役规则(仅 ADMIN,置 RETIRED)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> retire(@PathVariable Long id) {
        AuthContext.requireAdmin();
        ruleService.retire(id);
        return ApiResponse.ok();
    }
}
