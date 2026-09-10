package com.anes.schedule.service.roster;

import com.anes.schedule.service.roster.RosterIssue.FixType;
import com.anes.schedule.service.roster.RosterIssue.Level;
import com.anes.schedule.service.roster.RosterStaffMatcher.StaffMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 校验规则集(specs「校验分级与静默丢弃禁止」)。
 * 阻断:空表/岗位无法识别/人员未匹配(同名候选、停用)/日期缺失或越界/seq重号/月份不一致。
 * 警告:seq断号/值班四档缺人日/可疑同人同日多岗。
 * 合法形态:备班1×短接班顺序池首人同人不报(顺序池行无日期,天然不参与同人同日检查)。
 */
public final class RosterValidator {

    private static final Set<RosterPositionType> ON_CALL_TIERS = Set.of(
            RosterPositionType.ON_CALL_CHIEF, RosterPositionType.ON_CALL_T2,
            RosterPositionType.ON_CALL_T3, RosterPositionType.ON_CALL_T4);

    private RosterValidator() {
    }

    public static RosterValidationReport validate(String targetMonth, String fileMonth,
                                                  List<RosterRow> rows) {
        List<RosterIssue> issues = new ArrayList<>();

        RosterIssue monthIssue = RosterMonthRules.checkMonthConsistency(fileMonth, targetMonth);
        if (monthIssue != null) {
            issues.add(monthIssue);
        }
        if (rows.isEmpty()) {
            issues.add(RosterIssue.block("文件", "解析行集合为空(空表或仅表头)", FixType.NONE));
            return new RosterValidationReport(issues);
        }

        // 逐行:岗位识别 / 人员匹配 / 日期有效性
        Map<Integer, Set<RosterPositionType>> tiersByDay = new HashMap<>();
        Map<Integer, Map<Long, Integer>> staffPositionsByDay = new HashMap<>();
        for (RosterRow row : rows) {
            String loc = row.sheet() + ":第" + row.excelRow() + "行";
            if (row.type() == null) {
                issues.add(RosterIssue.block(loc, "岗位无法识别:" + row.label(), FixType.POSITION));
                continue;
            }
            StaffMatch match = row.match();
            if (match == null || !match.resolved()) {
                if (match != null && match.needsChoice()) {
                    issues.add(RosterIssue.block(loc, "同名人员需人工选择:" + row.cellText(),
                            FixType.STAFF_CANDIDATES));
                } else if (match != null && match.inactiveHit()) {
                    issues.add(RosterIssue.block(loc, "命中停用人员:" + row.cellText()
                            + "(按未匹配处理,请修正)", FixType.STAFF_CANDIDATES));
                } else {
                    issues.add(RosterIssue.block(loc, "人员未匹配:" + row.cellText(),
                            FixType.STAFF_CANDIDATES));
                }
            }
            if (row.type().form() == RosterForm.BY_DATE) {
                if (row.day() == null) {
                    issues.add(RosterIssue.block(loc, "按日岗位缺日期:" + row.label(), FixType.DATE));
                } else if (!RosterMonthRules.isValidDay(targetMonth, row.day())) {
                    issues.add(RosterIssue.block(loc,
                            "日期越界:" + targetMonth + "-" + row.day(), FixType.DATE));
                } else {
                    if (ON_CALL_TIERS.contains(row.type())) {
                        tiersByDay.computeIfAbsent(row.day(), k -> new HashSet<>()).add(row.type());
                    }
                    if (match != null && match.resolved()) {
                        staffPositionsByDay
                                .computeIfAbsent(row.day(), k -> new HashMap<>())
                                .merge(match.staffId(), 1, Integer::sum);
                    }
                }
            }
        }

        // 顺序池:重号阻断 / 断号警告
        TreeSet<Integer> seqs = new TreeSet<>();
        Set<Integer> duplicated = new HashSet<>();
        for (RosterRow row : rows) {
            if (row.seq() != null && !seqs.add(row.seq())) {
                duplicated.add(row.seq());
            }
        }
        for (Integer seq : duplicated) {
            issues.add(RosterIssue.block("顺序池", "序号重号:" + seq, FixType.NONE));
        }
        if (!seqs.isEmpty()) {
            for (int i = 1; i <= seqs.last(); i++) {
                if (!seqs.contains(i)) {
                    issues.add(RosterIssue.warn("顺序池", "序号断号:缺 " + i));
                }
            }
        }

        // 值班四档缺人日(警告;周末/节假日口径待确认,默认警告)
        for (Map.Entry<Integer, Set<RosterPositionType>> e : tiersByDay.entrySet()) {
            if (e.getValue().size() < ON_CALL_TIERS.size()) {
                issues.add(RosterIssue.warn("日期" + e.getKey() + "日",
                        "值班四档不齐(" + e.getValue().size() + "/4)"));
            }
        }

        // 可疑同人同日多岗(仅按日行;顺序池无日期,备班1×短接班首人合法不报)
        for (Map.Entry<Integer, Map<Long, Integer>> day : staffPositionsByDay.entrySet()) {
            for (Map.Entry<Long, Integer> staff : day.getValue().entrySet()) {
                if (staff.getValue() > 1) {
                    issues.add(RosterIssue.warn("日期" + day.getKey() + "日",
                            "同人同日多岗:人员#" + staff.getKey() + " 当日承担 "
                                    + staff.getValue() + " 个按日岗位"));
                }
            }
        }

        return new RosterValidationReport(issues);
    }
}
