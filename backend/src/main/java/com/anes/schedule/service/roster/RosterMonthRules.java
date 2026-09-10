package com.anes.schedule.service.roster;

import java.time.LocalDate;

/** 月份/日期规则(specs「目标月份规则」):月份一致性阻断、按目标月实际天数校验(含闰年) */
public final class RosterMonthRules {

    private RosterMonthRules() {
    }

    /** 文件内月份与用户所选目标月不一致 → 阻断;文件月解析不出时跳过(不误报) */
    public static RosterIssue checkMonthConsistency(String fileMonth, String targetMonth) {
        if (fileMonth == null || fileMonth.isBlank() || fileMonth.equals(targetMonth)) {
            return null;
        }
        return RosterIssue.block("文件头",
                "文件月份 " + fileMonth + " 与所选目标月 " + targetMonth + " 不一致", RosterIssue.FixType.NONE);
    }

    /** day 是否为目标月(yyyy-MM)的实际有效日 */
    public static boolean isValidDay(String month, int day) {
        LocalDate first = LocalDate.parse(month + "-01");
        return day >= 1 && day <= first.lengthOfMonth();
    }
}
