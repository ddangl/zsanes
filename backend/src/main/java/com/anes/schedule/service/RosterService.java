package com.anes.schedule.service;

import com.anes.schedule.common.BusinessException;
import com.anes.schedule.common.ErrorCode;
import com.anes.schedule.dto.RosterDtos.ImportSummary;
import com.anes.schedule.dto.RosterDtos.MonthResp;
import com.anes.schedule.entity.MonthlyRoster;
import com.anes.schedule.entity.MonthlyRosterItem;
import com.anes.schedule.entity.Staff;
import com.anes.schedule.mapper.MonthlyRosterItemMapper;
import com.anes.schedule.mapper.MonthlyRosterMapper;
import com.anes.schedule.mapper.StaffMapper;
import com.anes.schedule.service.roster.RosterFormatAdapter;
import com.anes.schedule.service.roster.RosterForm;
import com.anes.schedule.service.roster.RosterItemAssembler;
import com.anes.schedule.service.roster.RosterParseResult;
import com.anes.schedule.service.roster.RosterStaffMatcher;
import com.anes.schedule.service.roster.RosterValidationReport;
import com.anes.schedule.service.roster.RosterValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 月度值班备班表编排:解析(适配层+校验器)、导入(复核→事务删重插)、按月查询。
 * 纯逻辑均在 service/roster 域内单测覆盖;本层只做组装与事务。
 */
@Service
@RequiredArgsConstructor
public class RosterService {

    private final MonthlyRosterMapper rosterMapper;
    private final MonthlyRosterItemMapper itemMapper;
    private final StaffMapper staffMapper;

    /** 阶段一:解析 + 校验,零写入 */
    public RosterParseResult parse(MultipartFile file, String month) throws IOException {
        RosterFormatAdapter adapter = new RosterFormatAdapter(new RosterStaffMatcher(dbDirectory()));
        RosterParseResult parsed = adapter.parse(file.getInputStream(), month);
        RosterValidationReport report = RosterValidator.validate(month, parsed.fileMonth(),
                parsed.rows());
        List<com.anes.schedule.service.roster.RosterIssue> merged = new ArrayList<>(parsed.issues());
        merged.addAll(report.issues());
        return new RosterParseResult(parsed.fileMonth(), parsed.rows(), merged);
    }

    /** 阶段二前置:复核回传行集合(服务端重新校验,不信任前端) */
    public RosterValidationReport validateForImport(String month,
            List<com.anes.schedule.service.roster.RosterRow> rows) {
        return RosterValidator.validate(month, null, rows);
    }

    /** 阶段二:事务内同月替换(删旧全量重插),导入即 ACTIVE */
    @Transactional
    public ImportSummary importRoster(String month,
            List<com.anes.schedule.service.roster.RosterRow> rows, Long userId) {
        RosterValidationReport report = validateForImport(month, rows);
        if (report.hasBlock()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在阻断项,整体拒绝、零部分提交");
        }
        MonthlyRoster roster = rosterMapper.selectOne(new LambdaQueryWrapper<MonthlyRoster>()
                .eq(MonthlyRoster::getRosterMonth, month));
        if (roster == null) {
            roster = new MonthlyRoster();
            roster.setRosterMonth(month);
            roster.setStatus("ACTIVE");
            roster.setCreatedBy(userId);
            rosterMapper.insert(roster);
        } else {
            itemMapper.delete(new LambdaQueryWrapper<MonthlyRosterItem>()
                    .eq(MonthlyRosterItem::getRosterId, roster.getId()));
        }
        List<MonthlyRosterItem> items = RosterItemAssembler.assemble(roster.getId(), month, rows);
        items.forEach(itemMapper::insert);
        return new ImportSummary(roster.getId(), items.size());
    }

    /** 按月查询:按日条目 + 顺序池(供步骤4引擎与前端) */
    public MonthResp getMonth(String month) {
        MonthlyRoster roster = rosterMapper.selectOne(new LambdaQueryWrapper<MonthlyRoster>()
                .eq(MonthlyRoster::getRosterMonth, month));
        if (roster == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该月无月表:" + month);
        }
        List<MonthlyRosterItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<MonthlyRosterItem>()
                        .eq(MonthlyRosterItem::getRosterId, roster.getId())
                        .orderByAsc(MonthlyRosterItem::getDutyDate)
                        .orderByAsc(MonthlyRosterItem::getSeq));
        Map<Long, String> names = staffNames(items);
        List<MonthResp.DayEntry> byDate = items.stream()
                .filter(i -> i.getDutyDate() != null)
                .map(i -> new MonthResp.DayEntry(i.getDutyDate().toString(), i.getPositionType(),
                        i.getStaffId(), names.get(i.getStaffId())))
                .toList();
        List<MonthResp.PoolEntry> pool = items.stream()
                .filter(i -> i.getDutyDate() == null)
                .map(i -> new MonthResp.PoolEntry(i.getSeq(), i.getStaffId(),
                        names.get(i.getStaffId())))
                .toList();
        return new MonthResp(roster.getId(), month, roster.getStatus(), byDate, pool);
    }

    private Map<Long, String> staffNames(List<MonthlyRosterItem> items) {
        List<Long> ids = items.stream().map(MonthlyRosterItem::getStaffId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return staffMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Staff::getId, Staff::getName, (a, b) -> a));
    }

    /** staff 表目录实现:供匹配器(含停用标记,匹配器内部过滤在职) */
    private RosterStaffMatcher.StaffDirectory dbDirectory() {
        return new RosterStaffMatcher.StaffDirectory() {
            @Override
            public List<RosterStaffMatcher.StaffRef> byName(String name) {
                return staffMapper.selectList(new LambdaQueryWrapper<Staff>()
                                .eq(Staff::getName, name))
                        .stream()
                        .map(s -> new RosterStaffMatcher.StaffRef(s.getId(), s.getEmpNo(),
                                s.getName(), Boolean.TRUE.equals(s.getActive())))
                        .toList();
            }

            @Override
            public java.util.Optional<RosterStaffMatcher.StaffRef> byEmpNo(String empNo) {
                Staff s = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                        .eq(Staff::getEmpNo, empNo));
                return java.util.Optional.ofNullable(s)
                        .map(x -> new RosterStaffMatcher.StaffRef(x.getId(), x.getEmpNo(),
                                x.getName(), Boolean.TRUE.equals(x.getActive())));
            }
        };
    }
}
