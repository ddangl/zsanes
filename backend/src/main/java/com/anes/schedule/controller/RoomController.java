package com.anes.schedule.controller;

import com.anes.schedule.common.ApiResponse;
import com.anes.schedule.common.AuthContext;
import com.anes.schedule.common.PageResult;
import com.anes.schedule.dto.RoomDtos.RoomCreateReq;
import com.anes.schedule.dto.RoomDtos.RoomQuery;
import com.anes.schedule.dto.RoomDtos.RoomResp;
import com.anes.schedule.dto.RoomDtos.RoomUpdateReq;
import com.anes.schedule.service.RoomService;
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

@Tag(name = "手术室房间")
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "分页查询(可按区域/房号关键字筛选)")
    @GetMapping
    public ApiResponse<PageResult<RoomResp>> page(RoomQuery query) {
        return ApiResponse.ok(roomService.page(query));
    }

    @Operation(summary = "区域去重列表(下拉)")
    @GetMapping("/areas")
    public ApiResponse<List<String>> areas() {
        return ApiResponse.ok(roomService.areas());
    }

    @Operation(summary = "房间详情")
    @GetMapping("/{id}")
    public ApiResponse<RoomResp> get(@PathVariable Long id) {
        return ApiResponse.ok(roomService.get(id));
    }

    @Operation(summary = "新增房间(仅 ADMIN)")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody @Valid RoomCreateReq req) {
        AuthContext.requireAdmin();
        roomService.create(req);
        return ApiResponse.ok();
    }

    @Operation(summary = "修改房间(仅 ADMIN)")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid RoomUpdateReq req) {
        AuthContext.requireAdmin();
        roomService.update(id, req);
        return ApiResponse.ok();
    }

    @Operation(summary = "停用房间(仅 ADMIN,逻辑删除)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AuthContext.requireAdmin();
        roomService.delete(id);
        return ApiResponse.ok();
    }
}
