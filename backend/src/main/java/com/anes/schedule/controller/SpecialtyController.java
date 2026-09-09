package com.anes.schedule.controller;

import com.anes.schedule.common.ApiResponse;
import com.anes.schedule.common.AuthContext;
import com.anes.schedule.dto.SpecialtyDtos.SpecialtyResp;
import com.anes.schedule.dto.SpecialtyDtos.SpecialtySaveReq;
import com.anes.schedule.service.SpecialtyService;
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

import java.util.List;

@Tag(name = "亚专科")
@RestController
@RequestMapping("/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @Operation(summary = "全部亚专科(onlyActive=true 只看启用)")
    @GetMapping
    public ApiResponse<List<SpecialtyResp>> list(@RequestParam(defaultValue = "true") boolean onlyActive) {
        return ApiResponse.ok(specialtyService.listAll(onlyActive));
    }

    @Operation(summary = "新增(仅 ADMIN)")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody @Valid SpecialtySaveReq req) {
        AuthContext.requireAdmin();
        specialtyService.create(req);
        return ApiResponse.ok();
    }

    @Operation(summary = "修改(仅 ADMIN)")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid SpecialtySaveReq req) {
        AuthContext.requireAdmin();
        specialtyService.update(id, req);
        return ApiResponse.ok();
    }

    @Operation(summary = "停用(仅 ADMIN)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthContext.requireAdmin();
        specialtyService.delete(id);
        return ApiResponse.ok();
    }
}
