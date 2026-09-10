package com.anes.schedule.service.roster;

import com.anes.schedule.service.roster.RosterStaffMatcher.StaffMatch;

/**
 * 解析行:适配层产出的最小单元(sheet+物理行号定位,label 原文,type/day/seq 按形态,
 * match 为人员匹配结果)。parse 返回它,import 回传它(修正后),前后端共用契约。
 */
public record RosterRow(String sheet, int excelRow, String label, RosterPositionType type,
                        Integer day, Integer seq, String cellText, StaffMatch match) {
}
