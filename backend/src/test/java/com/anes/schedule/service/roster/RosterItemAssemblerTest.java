package com.anes.schedule.service.roster;

import com.anes.schedule.entity.MonthlyRosterItem;
import org.junit.jupiter.api.Test;

import com.anes.schedule.service.roster.RosterStaffMatcher.StaffMatch;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 行→落库实体组装(tasks 4.2 前置纯逻辑):
 * 按日行 → duty_date=目标月-日;顺序池行 → duty_date=null + seq;未匹配行组装即拒绝(防御)。
 */
class RosterItemAssemblerTest {

    private static RosterRow row(RosterPositionType type, Integer day, Integer seq, Long staffId) {
        return new RosterRow("S1", 5, type.name(), type, day, seq, "某单元格",
                staffId == null ? StaffMatch.unresolved()
                        : new StaffMatch(staffId, List.of(), false, false));
    }

    @Test
    void byDateRowAssemblesWithDutyDate() {
        List<MonthlyRosterItem> items = RosterItemAssembler.assemble(7L, "2026-09",
                List.of(row(RosterPositionType.STANDBY1, 3, null, 5L)));
        assertEquals(1, items.size());
        MonthlyRosterItem item = items.get(0);
        assertEquals(7L, item.getRosterId());
        assertEquals(java.time.LocalDate.of(2026, 9, 3), item.getDutyDate());
        assertEquals("STANDBY1", item.getPositionType());
        assertEquals(5L, item.getStaffId());
        assertEquals(0, item.getSeq());
    }

    @Test
    void poolRowAssemblesWithSeqAndNullDate() {
        List<MonthlyRosterItem> items = RosterItemAssembler.assemble(7L, "2026-09",
                List.of(row(RosterPositionType.SHORT_RELIEF, null, 2, 9L)));
        MonthlyRosterItem item = items.get(0);
        assertEquals(null, item.getDutyDate());
        assertEquals(2, item.getSeq());
        assertEquals("SHORT_RELIEF", item.getPositionType());
    }

    @Test
    void unresolvedRowRejectedDefensively() {
        assertThrows(IllegalStateException.class,
                () -> RosterItemAssembler.assemble(7L, "2026-09",
                        List.of(row(RosterPositionType.ON_CALL_CHIEF, 1, null, null))));
    }
}
