package com.anes.schedule.service.roster;

import com.anes.schedule.entity.MonthlyRosterItem;

import java.time.LocalDate;
import java.util.List;

/**
 * 行→落库实体组装(纯逻辑):按日行写 duty_date=目标月-日、seq=0;
 * 顺序池行写 duty_date=null、seq=列序;未匹配行组装即拒绝(import 复核后的防御)。
 */
public final class RosterItemAssembler {

    private RosterItemAssembler() {
    }

    public static List<MonthlyRosterItem> assemble(Long rosterId, String month, List<RosterRow> rows) {
        return rows.stream().map(row -> {
            if (row.match() == null || !row.match().resolved()) {
                throw new IllegalStateException(
                        "存在未匹配行,禁止组装:" + row.sheet() + " 第" + row.excelRow() + "行");
            }
            MonthlyRosterItem item = new MonthlyRosterItem();
            item.setRosterId(rosterId);
            item.setPositionType(row.type().name());
            item.setStaffId(row.match().staffId());
            if (row.type().form() == RosterForm.BY_DATE) {
                item.setDutyDate(LocalDate.parse(
                        month + "-" + String.format("%02d", row.day())));
                item.setSeq(0);
            } else {
                item.setDutyDate(null);
                item.setSeq(row.seq());
            }
            return item;
        }).toList();
    }
}
