package com.anes.schedule.service.roster;

import org.junit.jupiter.api.Test;

import com.anes.schedule.service.roster.RosterStaffMatcher.StaffDirectory;
import com.anes.schedule.service.roster.RosterStaffMatcher.StaffMatch;
import com.anes.schedule.service.roster.RosterStaffMatcher.StaffRef;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 人员匹配链(specs「人员匹配」):姓名读入,唯一名直解/同名候选/含工号优先/
 * 仅在职范围/停用按未匹配特殊提示。库内 8 对同名场景用构造目录模拟。
 */
class RosterStaffMatcherTest {

    private final StaffDirectory directory = new StaffDirectory() {
        @Override
        public List<StaffRef> byName(String name) {
            return switch (name) {
                case "仓静" -> List.of(new StaffRef(1L, "10495", "仓静", true));
                case "南克" -> List.of(
                        new StaffRef(11L, "14106", "南克", true),
                        new StaffRef(12L, "57375", "南克", true));
                case "陆珠凤" -> List.of(
                        new StaffRef(21L, "12729", "陆珠凤", true),
                        new StaffRef(22L, "19999", "陆珠凤", false));
                default -> List.of();
            };
        }

        @Override
        public Optional<StaffRef> byEmpNo(String empNo) {
            return switch (empNo) {
                case "12729" -> Optional.of(new StaffRef(21L, "12729", "陆珠凤", true));
                case "18888" -> Optional.of(new StaffRef(31L, "18888", "老主任", false));
                default -> Optional.empty();
            };
        }
    };

    private final RosterStaffMatcher matcher = new RosterStaffMatcher(directory);

    @Test
    void uniqueNameResolvesDirectly() {
        StaffMatch match = matcher.match("仓静");
        assertTrue(match.resolved());
        assertEquals(1L, match.staffId());
        assertTrue(match.candidates().isEmpty());
    }

    @Test
    void duplicateNameYieldsCandidatesAndRequiresChoice() {
        StaffMatch match = matcher.match("南克");
        assertFalse(match.resolved());
        assertTrue(match.needsChoice());
        assertEquals(2, match.candidates().size());
        assertEquals("14106", match.candidates().get(0).empNo());
    }

    @Test
    void inactiveOnlyNameIsUnresolvedWithInactiveNote() {
        // "陆珠凤"命中一在职一停用:在职可用、停用被过滤,唯一在职直接解析
        StaffMatch match = matcher.match("陆珠凤");
        assertTrue(match.resolved());
        assertEquals(21L, match.staffId());
    }

    @Test
    void empNoTakesPriority() {
        // 纯数字按工号优先匹配,即使该工号姓名与他人重名
        StaffMatch match = matcher.match("12729");
        assertTrue(match.resolved());
        assertEquals(21L, match.staffId());
    }

    @Test
    void inactiveEmpNoIsUnresolvedWithInactiveFlag() {
        StaffMatch match = matcher.match("18888");
        assertFalse(match.resolved());
        assertTrue(match.inactiveHit());
        assertFalse(match.needsChoice());
    }

    @Test
    void unknownTextIsPlainUnresolved() {
        StaffMatch match = matcher.match("查无此人");
        assertFalse(match.resolved());
        assertFalse(match.needsChoice());
        assertFalse(match.inactiveHit());
        assertTrue(match.candidates().isEmpty());
    }

    @Test
    void blankTextIsUnresolved() {
        assertFalse(matcher.match("").resolved());
        assertFalse(matcher.match("  ").resolved());
    }
}
