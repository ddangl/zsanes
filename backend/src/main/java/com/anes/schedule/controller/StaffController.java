package com.anes.schedule.controller;

import com.alibaba.excel.EasyExcel;
import com.anes.schedule.common.ApiResponse;
import com.anes.schedule.common.AuthContext;
import com.anes.schedule.common.PageResult;
import com.anes.schedule.dto.StaffDtos.ImportResult;
import com.anes.schedule.dto.StaffDtos.StaffLite;
import com.anes.schedule.dto.StaffDtos.StaffQuery;
import com.anes.schedule.dto.StaffDtos.StaffResp;
import com.anes.schedule.dto.StaffDtos.StaffSaveReq;
import com.anes.schedule.dto.StaffImportRow;
import com.anes.schedule.service.StaffService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "人员档案")
@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @Operation(summary = "分页查询(keyword 匹配姓名/工号,jobRole 精确,active 过滤)")
    @GetMapping
    public ApiResponse<PageResult<StaffResp>> page(StaffQuery query) {
        return ApiResponse.ok(staffService.page(query));
    }

    @Operation(summary = "全员轻量列表(带教下拉)")
    @GetMapping("/lite")
    public ApiResponse<List<StaffLite>> lite(@RequestParam(defaultValue = "true") boolean onlyActive) {
        return ApiResponse.ok(staffService.liteAll(onlyActive));
    }

    @Operation(summary = "下载导入模板(13 列,与科室 8 月表同构)")
    @GetMapping("/import/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename="
                + URLEncoder.encode("人员档案导入模板.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), StaffImportRow.class)
                .sheet("人员档案").doWrite(Collections.emptyList());
    }

    @Operation(summary = "Excel 批量导入(仅 ADMIN;工号 upsert,返回结果报告)")
    @PostMapping("/import")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        AuthContext.requireAdmin();
        if (file.isEmpty()) {
            throw com.anes.schedule.common.BusinessException.badRequest("上传文件为空");
        }
        return ApiResponse.ok(staffService.importExcel(file.getInputStream()));
    }

    @Operation(summary = "新增人员(仅 ADMIN)")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody @Valid StaffSaveReq req) {
        AuthContext.requireAdmin();
        staffService.create(req);
        return ApiResponse.ok();
    }

    @Operation(summary = "修改人员(仅 ADMIN)")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid StaffSaveReq req) {
        AuthContext.requireAdmin();
        staffService.update(id, req);
        return ApiResponse.ok();
    }

    @Operation(summary = "停用人员(仅 ADMIN,逻辑删除)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthContext.requireAdmin();
        staffService.delete(id);
        return ApiResponse.ok();
    }
}
