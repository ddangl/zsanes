package com.anes.schedule.service.roster;

import org.junit.jupiter.api.Test;

import com.anes.schedule.service.roster.RosterStaffMatcher.StaffMatch;
import com.anes.schedule.service.roster.RosterStaffMatcher.StaffRef;

import java.util.List;

import static com.anes.schedule.service.roster.RosterIssue.FixType.DATE;
import static com.anes.schedule.service.roster.RosterIssue.FixType.POSITION;
import static com.anes.schedule.service.roster.RosterIssue.FixType.STAFF_CANDIDATES;
import static com.anes.schedule.service.roster.RosterIssue.Level.BLOCK;
import static com.anes.schedule.service.roster.RosterIssue.Level.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验规则集(specs「校验分级与静默丢弃禁止」):
 * 阻断=空表/未知岗位/未匹配/日期越界/seq重号;警告=同名提示外的断号/四档缺人/可疑同人同日多岗;
 * 合法形态(备班1×短接班顺序池首人)不报。
 */
class RosterValidatorTest {

    private static final String MONTH = "2026-09";

    /** 构造一行(按日岗位) */
    private static RosterRow dayRow(String sheet, int row, RosterPositionType type, int day,
                                    Long staffId) {
        return new RosterRow(sheet, row, type == null ? null : type.name(), type, day, null,
                "某单元格", staffId == null
                        ? StaffMatch.unresolved()
                        : new StaffMatch(staffId, List.of(), false, false));
    }

    /** 构造一行(顺序池) */
    private static RosterRow poolRow(int seq, Long staffId) {
        return new RosterRow("S1", 90 + seq, "短接班", RosterPositionType.SHORT_RELIEF, null, seq,
                "某单元格", new StaffMatch(staffId, List.of(), false, false));
    }

    @Test
    void emptyRowsProduceBlock() {
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of());
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == BLOCK && i.message().contains("空")));
    }

    @Test
    void unknownPositionProducesBlockWithPositionFix() {
        RosterRow row = new RosterRow("S1", 5, "外星岗", null, 3, null, "张三",
                new StaffMatch(1L, List.of(), false, false));
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(row));
        assertTrue(report.issues().stream().anyMatch(
                i -> i.level() == BLOCK && i.fixType() == POSITION && i.location().contains("5")));
        assertTrue(report.hasBlock());
    }

    @Test
    void unresolvedOrChoiceOrInactiveMatchProducesBlockWithCandidatesFix() {
        RosterRow unresolved = dayRow("S1", 6, RosterPositionType.ON_CALL_T2, 3, null);
        RosterRow needsChoice = new RosterRow("S1", 7, "备班1", RosterPositionType.STANDBY1, 3,
                null, "南克", new StaffMatch(null, List.of(new StaffRef(1L, "1", "南克", true)),
                true, false));
        RosterRow inactive = new RosterRow("S1", 8, "老总", RosterPositionType.ON_CALL_CHIEF, 3,
                null, "18888", new StaffMatch(null, List.of(), false, true));

        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH,
                List.of(unresolved, needsChoice, inactive));
        List<RosterIssue> blocks = report.issues().stream()
                .filter(i -> i.level() == BLOCK && i.fixType() == STAFF_CANDIDATES).toList();
        assertEquals(3, blocks.size());
        assertTrue(blocks.stream().anyMatch(i -> i.message().contains("停用")));
    }

    @Test
    void invalidDayProducesBlockWithDateFix() {
        RosterRow row = dayRow("S1", 9, RosterPositionType.STANDBY1, 31, 1L); // 9月无31日
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(row));
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == BLOCK && i.fixType() == DATE));
    }

    @Test
    void seqDuplicateBlocksAndGapWarns() {
        RosterRow a = poolRow(1, 1L);
        RosterRow b = poolRow(1, 2L);   // 重号 → 阻断
        RosterRow c = poolRow(3, 3L);   // 断号(缺2) → 警告
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(a, b, c));
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == BLOCK && i.message().contains("重号")));
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == WARN && i.message().contains("断号")));
    }

    @Test
    void missingTierDayWarns() {
        // 9月3日只有三档+四档,缺老总/二档 → 四档缺人警告
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(
                dayRow("S1", 10, RosterPositionType.ON_CALL_T3, 3, 3L),
                dayRow("S1", 11, RosterPositionType.ON_CALL_T4, 3, 4L)));
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == WARN && i.message().contains("四档")));
    }

    @Test
    void completeTierDayProducesNoTierWarning() {
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(
                dayRow("S1", 10, RosterPositionType.ON_CALL_CHIEF, 3, 1L),
                dayRow("S1", 11, RosterPositionType.ON_CALL_T2, 3, 2L),
                dayRow("S1", 12, RosterPositionType.ON_CALL_T3, 3, 3L),
                dayRow("S1", 13, RosterPositionType.ON_CALL_T4, 3, 4L)));
        assertFalse(report.issues().stream().anyMatch(i -> i.message().contains("四档")));
    }

    @Test
    void samePersonSameDayMultiRoleWarns() {
        // 员工5 同日既值班二档又当备班1 → 可疑同人同日多岗警告
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(
                dayRow("S1", 10, RosterPositionType.ON_CALL_T2, 3, 5L),
                dayRow("S1", 11, RosterPositionType.STANDBY1, 3, 5L)));
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == WARN && i.message().contains("同人同日")));
    }

    @Test
    void standby1AndShortPoolFirstIsLegalAndNotFlagged() {
        // 备班1(按日)与短接班顺序池首人为同一人 = 合法业务形态,不报同人同日多岗
        RosterValidationReport report = RosterValidator.validate(MONTH, MONTH, List.of(
                dayRow("S1", 10, RosterPositionType.ON_CALL_CHIEF, 3, 1L),
                dayRow("S1", 11, RosterPositionType.ON_CALL_T2, 3, 2L),
                dayRow("S1", 12, RosterPositionType.ON_CALL_T3, 3, 3L),
                dayRow("S1", 13, RosterPositionType.ON_CALL_T4, 3, 4L),
                dayRow("S1", 14, RosterPositionType.STANDBY1, 3, 5L),
                poolRow(1, 5L)));
        assertFalse(report.issues().stream().anyMatch(i -> i.message().contains("同人同日")));
        assertFalse(report.hasBlock());
    }

    @Test
    void monthMismatchFromRulesIncluded() {
        RosterRow row = dayRow("S1", 10, RosterPositionType.ON_CALL_CHIEF, 3, 1L);
        RosterValidationReport report = RosterValidator.validate(MONTH, "2026-08", List.of(row));
        assertTrue(report.issues().stream()
                .anyMatch(i -> i.level() == BLOCK && i.message().contains("不一致")));
    }
}
