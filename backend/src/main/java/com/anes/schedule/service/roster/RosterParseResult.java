package com.anes.schedule.service.roster;

import java.util.List;

/** 解析阶段产物:文件月(仅一致性校验用)+ 行集合 + 适配层自产问题(无法解析/岗位无法识别) */
public record RosterParseResult(String fileMonth, List<RosterRow> rows, List<RosterIssue> issues) {
}
