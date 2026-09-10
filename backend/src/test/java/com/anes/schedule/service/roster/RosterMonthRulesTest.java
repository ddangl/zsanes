package com.anes.schedule.service.roster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 月份/日期规则(specs「目标月份规则」):
 * 文件月≠目标月=阻断;日期有效性相对目标月实际天数(含闰年二月)。
 */
class RosterMonthRulesTest {

    @Test
    void monthMismatchProducesBlockIssue() {
        RosterIssue issue = RosterMonthRules.checkMonthConsistency("2026-08", "2026-09");
        assertEquals(RosterIssue.Level.BLOCK, issue.level());
        assertEquals(RosterIssue.FixType.NONE, issue.fixType());
        assertTrue(issue.message().contains("2026-08"));
        assertTrue(issue.message().contains("2026-09"));
    }

    @Test
    void monthMatchProducesNoIssue() {
        assertNull(RosterMonthRules.checkMonthConsistency("2026-09", "2026-09"));
    }

    @Test
    void fileMonthUnparsableIsIgnored() {
        // 文件内月份仅做一致性校验;解析不出时不产生误报(由适配层决定是否报无法解析)
        assertNull(RosterMonthRules.checkMonthConsistency(null, "2026-09"));
    }

    @Test
    void dayOutOfRangeIsInvalid() {
        assertFalse(RosterMonthRules.isValidDay("2026-09", 31), "9月没有31日");
        assertFalse(RosterMonthRules.isValidDay("2026-02", 29), "2026非闰年,2月无29日");
        assertFalse(RosterMonthRules.isValidDay("2026-04", 31), "4月只有30天");
        assertFalse(RosterMonthRules.isValidDay("2026-09", 0), "0日非法");
    }

    @Test
    void validDaysIncludingLeapYear() {
        assertTrue(RosterMonthRules.isValidDay("2026-09", 30));
        assertTrue(RosterMonthRules.isValidDay("2026-02", 28));
        assertTrue(RosterMonthRules.isValidDay("2028-02", 29), "2028是闰年");
    }
}
