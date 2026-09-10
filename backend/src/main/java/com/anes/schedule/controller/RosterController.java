package com.anes.schedule.controller;

import com.anes.schedule.common.ApiResponse;
import com.anes.schedule.common.AuthContext;
import com.anes.schedule.common.ErrorCode;
import com.anes.schedule.dto.RosterDtos.ImportSummary;
import com.anes.schedule.dto.RosterDtos.MonthResp;
import com.anes.schedule.dto.RosterDtos.ParseResp;
import com.anes.schedule.service.RosterService;
import com.anes.schedule.service.roster.RosterIssue;
import com.anes.schedule.service.roster.RosterParseResult;
import com.anes.schedule.service.roster.RosterRow;
import com.anes.schedule.service.roster.RosterValidationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "月度值班备班表")
@RestController
@RequestMapping("/roster")
@RequiredArgsConstructor
public class RosterController {

    private final RosterService rosterService;

    @Operation(summary = "阶段一:上传解析(仅 ADMIN,零写入),返回行集合+问题清单")
    @PostMapping("/parse")
    public ApiResponse<ParseResp> parse(@RequestParam("month") String month,
                                        @RequestParam("file") MultipartFile file) throws IOException {
        AuthContext.requireAdmin();
        RosterParseResult result = rosterService.parse(file, month);
        boolean hasBlock = result.issues().stream()
                .anyMatch(i -> i.level() == RosterIssue.Level.BLOCK);
        return ApiResponse.ok(new ParseResp(result.fileMonth(), result.rows(),
                result.issues(), hasBlock));
    }

    @Operation(summary = "阶段二:回传修正后行集合并入库(仅 ADMIN;阻断则 400+问题清单,零部分提交)")
    @PostMapping("/import")
    public ApiResponse<Object> importRoster(@RequestParam("month") String month,
                                            @RequestBody List<RosterRow> rows) {
        Long userId = AuthContext.requireAdmin().userId();
        RosterValidationReport report = rosterService.validateForImport(month, rows);
        if (report.hasBlock()) {
            return ApiResponse.of(ErrorCode.BAD_REQUEST.getCode(),
                    "存在阻断项,整体拒绝、零部分提交", report.issues());
        }
        ImportSummary summary = rosterService.importRoster(month, rows, userId);
        return ApiResponse.ok(summary);
    }

    @Operation(summary = "按月查询(按日条目 + 顺序池)")
    @GetMapping("/{month}")
    public ApiResponse<MonthResp> getMonth(@PathVariable String month) {
        return ApiResponse.ok(rosterService.getMonth(month));
    }
}
