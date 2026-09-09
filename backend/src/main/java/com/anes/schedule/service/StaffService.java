package com.anes.schedule.service;

import com.alibaba.excel.EasyExcel;
import com.anes.schedule.common.BusinessException;
import com.anes.schedule.common.PageResult;
import com.anes.schedule.dto.StaffDtos.ImportResult;
import com.anes.schedule.dto.StaffDtos.StaffLite;
import com.anes.schedule.dto.StaffDtos.StaffQuery;
import com.anes.schedule.dto.StaffDtos.StaffResp;
import com.anes.schedule.dto.StaffDtos.StaffSaveReq;
import com.anes.schedule.dto.StaffImportRow;
import com.anes.schedule.entity.Specialty;
import com.anes.schedule.entity.Staff;
import com.anes.schedule.mapper.SpecialtyMapper;
import com.anes.schedule.mapper.StaffMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 人员档案:手工 CRUD + Excel 批量导入。
 * 导入约定(业务规则 2.1 / 假设 A6):工号为唯一键 upsert;
 * 带教"教师行挂学生工号"导入后转为学生行 mentor_id;同名不同工号不阻断但提示。
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffMapper staffMapper;
    private final SpecialtyMapper specialtyMapper;

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    public PageResult<StaffResp> page(StaffQuery query) {
        LambdaQueryWrapper<Staff> qw = new LambdaQueryWrapper<Staff>()
                .and(StringUtils.hasText(query.keyword()), w -> w
                        .like(Staff::getName, query.keyword())
                        .or().like(Staff::getEmpNo, query.keyword()))
                .eq(StringUtils.hasText(query.jobRole()), Staff::getJobRole, query.jobRole())
                .eq(query.active() != null, Staff::getActive, query.active())
                .orderByAsc(Staff::getJobRole).orderByAsc(Staff::getEmpNo);
        Page<Staff> page = staffMapper.selectPage(Page.of(query.page(), query.size()), qw);

        Map<Long, String> specialtyNames = specialtyNameMap();
        Map<Long, String> staffNames = page.getRecords().stream()
                .filter(s -> s.getMentorId() != null)
                .map(Staff::getMentorId).distinct()
                .collect(Collectors.toMap(Function.identity(),
                        id -> {
                            Staff m = staffMapper.selectById(id);
                            return m == null ? "?" : m.getName();
                        }));
        List<StaffResp> list = page.getRecords().stream()
                .map(s -> toResp(s, specialtyNames, staffNames)).toList();
        return PageResult.of(page.getTotal(), list);
    }

    /** 全员轻量列表(带教选择下拉等) */
    public List<StaffLite> liteAll(boolean onlyActive) {
        LambdaQueryWrapper<Staff> qw = new LambdaQueryWrapper<Staff>()
                .eq(onlyActive, Staff::getActive, true)
                .orderByAsc(Staff::getJobRole).orderByAsc(Staff::getEmpNo);
        return staffMapper.selectList(qw).stream()
                .map(s -> new StaffLite(s.getId(), s.getEmpNo(), s.getName(), s.getJobRole()))
                .toList();
    }

    // ------------------------------------------------------------------
    // 手工维护
    // ------------------------------------------------------------------

    public void create(StaffSaveReq req) {
        assertEmpNoFree(req.empNo(), null);
        Staff s = new Staff();
        applySave(s, req);
        staffMapper.insert(s);
    }

    public void update(Long id, StaffSaveReq req) {
        Staff s = requireStaff(id);
        assertEmpNoFree(req.empNo(), id);
        applySave(s, req);
        staffMapper.updateById(s);
    }

    /** 停用(逻辑删除,保留历史排班引用) */
    public void delete(Long id) {
        Staff s = requireStaff(id);
        s.setActive(false);
        staffMapper.updateById(s);
    }

    // ------------------------------------------------------------------
    // Excel 导入
    // ------------------------------------------------------------------

    @Transactional
    public ImportResult importExcel(InputStream in) {
        List<StaffImportRow> rows = EasyExcel.read(in).head(StaffImportRow.class)
                .sheet().doReadSync();
        ImportResult result = new ImportResult();
        result.setTotalRows(rows.size());

        // 导入前快照:工号→人员、姓名→工号(用于同名提示)
        Map<String, Staff> byEmpNo = staffMapper.selectList(null).stream()
                .collect(Collectors.toMap(s -> s.getEmpNo() == null ? "" : s.getEmpNo(),
                        Function.identity(), (a, b) -> a, HashMap::new));
        Map<String, List<String>> nameToEmpNos = byEmpNo.values().stream()
                .collect(Collectors.groupingBy(Staff::getName,
                        Collectors.mapping(s -> s.getEmpNo() == null ? "" : s.getEmpNo(),
                                Collectors.toList())));

        // 亚专科名→id,不存在的自动新建(受控词表来自科室表)
        Map<String, Long> specialtyIds = specialtyMapper.selectList(null).stream()
                .collect(Collectors.toMap(Specialty::getName, Specialty::getId));

        for (StaffImportRow row : rows) {
            normalize(row);
            if (!StringUtils.hasText(row.getEmpNo()) || !StringUtils.hasText(row.getName())
                    || !StringUtils.hasText(row.getJobRole())) {
                result.setSkipped(result.getSkipped() + 1);
                result.warn("跳过(工号/姓名/职称缺失):" + row.getName() + " " + row.getEmpNo());
                continue;
            }
            // 同名不同工号提示(不阻断,假设 A6)
            List<String> sameNameEmpNos = nameToEmpNos.get(row.getName());
            if (sameNameEmpNos != null && !sameNameEmpNos.contains(row.getEmpNo())) {
                result.warn("同名不同工号:" + row.getName() + "(库内#" + String.join("/", sameNameEmpNos)
                        + " vs 导入#" + row.getEmpNo() + "),已按工号区分");
            }

            Staff s = byEmpNo.get(row.getEmpNo());
            boolean isNew = s == null;
            if (isNew) {
                s = new Staff();
                s.setEmpNo(row.getEmpNo());
                s.setActive(true);
            }
            s.setName(row.getName());
            s.setJobRole(row.getJobRole());
            s.setTitle(row.getTitle());
            s.setGrade(row.getGrade());
            s.setSaturdayWork(parseSaturday(row.getSaturdayWork()));
            s.setNote(row.getNote());
            s.setPartTimeRule(derivePartTimeRule(row.getNote()));
            s.setSpecialty1Id(resolveSpecialty(row.getSpecialty1Name(), specialtyIds, result));
            s.setSpecialty2Id(resolveSpecialty(row.getSpecialty2Name(), specialtyIds, result));
            if (isNew) {
                staffMapper.insert(s);
                byEmpNo.put(s.getEmpNo(), s);
                result.setInserted(result.getInserted() + 1);
            } else {
                staffMapper.updateById(s);
                result.setUpdated(result.getUpdated() + 1);
            }
        }

        // 第二遍:带教关系——教师行挂学生工号 → 学生行 mentor_id
        for (StaffImportRow row : rows) {
            normalize(row);
            Staff teacher = byEmpNo.get(row.getEmpNo());
            if (teacher == null) {
                continue;
            }
            for (String studentEmpNo : new String[]{row.getMentor1EmpNo(), row.getMentor2EmpNo(),
                    row.getMentor3EmpNo()}) {
                if (!StringUtils.hasText(studentEmpNo)) {
                    continue;
                }
                Staff student = byEmpNo.get(studentEmpNo);
                if (student == null) {
                    result.warn("带教学生工号未找到:" + studentEmpNo + "(教师:" + teacher.getName() + ")");
                } else if (!Objects.equals(student.getId(), teacher.getId())) {
                    student.setMentorId(teacher.getId());
                    staffMapper.updateById(student);
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 数字单元格可能读成 "10257.0",去掉尾部小数 */
    public static String normalizeEmpNo(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        return v.endsWith(".0") ? v.substring(0, v.length() - 2) : v;
    }

    private static void normalize(StaffImportRow row) {
        row.setEmpNo(normalizeEmpNo(row.getEmpNo()));
        row.setMentor1EmpNo(normalizeEmpNo(row.getMentor1EmpNo()));
        row.setMentor2EmpNo(normalizeEmpNo(row.getMentor2EmpNo()));
        row.setMentor3EmpNo(normalizeEmpNo(row.getMentor3EmpNo()));
    }

    private static Boolean parseSaturday(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        if ("否".equals(raw.trim()) || "0".equals(raw.trim())) {
            return false;
        }
        if ("是".equals(raw.trim()) || "1".equals(raw.trim())) {
            return true;
        }
        return null;
    }

    /** 周期性缺席启发式:特殊说明含 周/半天 视为周期规则(待确认 #12) */
    private static String derivePartTimeRule(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        return note.contains("周") || note.contains("半天") ? note : null;
    }

    private Long resolveSpecialty(String name, Map<String, Long> specialtyIds, ImportResult result) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        Long id = specialtyIds.get(name.trim());
        if (id == null) {
            Specialty sp = new Specialty();
            sp.setName(name.trim());
            sp.setSort(99);
            sp.setActive(true);
            specialtyMapper.insert(sp);
            specialtyIds.put(sp.getName(), sp.getId());
            result.warn("自动新建亚专科:" + sp.getName());
            id = sp.getId();
        }
        return id;
    }

    private Map<Long, String> specialtyNameMap() {
        return specialtyMapper.selectList(null).stream()
                .collect(Collectors.toMap(Specialty::getId, Specialty::getName));
    }

    private StaffResp toResp(Staff s, Map<Long, String> specialtyNames, Map<Long, String> staffNames) {
        return new StaffResp(s.getId(), s.getEmpNo(), s.getName(), s.getJobRole(), s.getTitle(),
                s.getSubLevel(), s.getSpecialty1Id(), s.getSpecialty2Id(),
                specialtyNames.get(s.getSpecialty1Id()), specialtyNames.get(s.getSpecialty2Id()),
                s.getMentorId(), s.getMentorId() == null ? null : staffNames.get(s.getMentorId()),
                s.getGrade(), s.getSaturdayWork(), s.getPartTimeRule(), s.getNote(), s.getPhone(),
                s.getActive());
    }

    private void applySave(Staff s, StaffSaveReq req) {
        s.setEmpNo(StringUtils.hasText(req.empNo()) ? req.empNo().trim() : null);
        s.setName(req.name());
        s.setJobRole(req.jobRole());
        s.setTitle(req.title());
        s.setSubLevel(req.subLevel());
        s.setSpecialty1Id(req.specialty1Id());
        s.setSpecialty2Id(req.specialty2Id());
        s.setMentorId(req.mentorId());
        s.setGrade(req.grade());
        s.setSaturdayWork(req.saturdayWork());
        s.setPartTimeRule(req.partTimeRule());
        s.setNote(req.note());
        s.setPhone(req.phone());
        s.setActive(req.active() == null || req.active());
    }

    private Staff requireStaff(Long id) {
        Staff s = staffMapper.selectById(id);
        if (s == null) {
            throw BusinessException.badRequest("人员不存在:id=" + id);
        }
        return s;
    }

    private void assertEmpNoFree(String empNo, Long excludeId) {
        if (!StringUtils.hasText(empNo)) {
            return;
        }
        Long count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getEmpNo, empNo.trim())
                .ne(excludeId != null, Staff::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("工号已存在:" + empNo);
        }
    }
}
