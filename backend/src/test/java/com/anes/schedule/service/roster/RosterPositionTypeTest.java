package com.anes.schedule.service.roster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 岗位映射(specs/monthly-roster:10 类 position_type,按日/顺序池双形态)。
 * 对应 tasks 1.2:映射表 4(值班档)+2(备班)+4(接班/中班)全覆盖。
 */
class RosterPositionTypeTest {

    @Test
    void tenTypesExistWithExactNames() {
        assertEquals(10, RosterPositionType.values().length);
        assertEquals("ON_CALL_CHIEF", RosterPositionType.ON_CALL_CHIEF.name());
        assertEquals("ON_CALL_T2", RosterPositionType.ON_CALL_T2.name());
        assertEquals("ON_CALL_T3", RosterPositionType.ON_CALL_T3.name());
        assertEquals("ON_CALL_T4", RosterPositionType.ON_CALL_T4.name());
        assertEquals("STANDBY1", RosterPositionType.STANDBY1.name());
        assertEquals("STANDBY2", RosterPositionType.STANDBY2.name());
        assertEquals("SHORT_RELIEF", RosterPositionType.SHORT_RELIEF.name());
        assertEquals("LONG_RELIEF", RosterPositionType.LONG_RELIEF.name());
        assertEquals("ASSISTANT_RELIEF", RosterPositionType.ASSISTANT_RELIEF.name());
        assertEquals("ASSISTANT_MID", RosterPositionType.ASSISTANT_MID.name());
    }

    @Test
    void formsByDateVersusPool() {
        // 按日固定:值班四档 + 备班1/2 + 长接班/副麻接班/副麻中班(三类默认约定,design D6)
        RosterPositionType[] byDate = {
                RosterPositionType.ON_CALL_CHIEF, RosterPositionType.ON_CALL_T2,
                RosterPositionType.ON_CALL_T3, RosterPositionType.ON_CALL_T4,
                RosterPositionType.STANDBY1, RosterPositionType.STANDBY2,
                RosterPositionType.LONG_RELIEF, RosterPositionType.ASSISTANT_RELIEF,
                RosterPositionType.ASSISTANT_MID};
        for (RosterPositionType type : byDate) {
            assertEquals(RosterForm.BY_DATE, type.form(), type + " 应为按日形态");
        }
        // 顺序池:仅短接班(唯一明确有序,业务规则"按照接班顺序依次排")
        assertEquals(RosterForm.SEQUENTIAL_POOL, RosterPositionType.SHORT_RELIEF.form());
    }

    @Test
    void resolvesChineseLabels() {
        assertEquals(RosterPositionType.ON_CALL_CHIEF, RosterPositionType.fromLabel("值班老总"));
        assertEquals(RosterPositionType.ON_CALL_CHIEF, RosterPositionType.fromLabel("老总"));
        assertEquals(RosterPositionType.ON_CALL_T2, RosterPositionType.fromLabel("二档"));
        assertEquals(RosterPositionType.ON_CALL_T3, RosterPositionType.fromLabel("三档"));
        assertEquals(RosterPositionType.ON_CALL_T4, RosterPositionType.fromLabel("四档"));
        assertEquals(RosterPositionType.STANDBY1, RosterPositionType.fromLabel("备班1"));
        assertEquals(RosterPositionType.STANDBY2, RosterPositionType.fromLabel("备班2"));
        assertEquals(RosterPositionType.SHORT_RELIEF, RosterPositionType.fromLabel("短接班"));
        assertEquals(RosterPositionType.LONG_RELIEF, RosterPositionType.fromLabel("长接班"));
        assertEquals(RosterPositionType.ASSISTANT_RELIEF, RosterPositionType.fromLabel("副麻接班"));
        assertEquals(RosterPositionType.ASSISTANT_MID, RosterPositionType.fromLabel("副麻中班"));
    }

    @Test
    void unknownLabelResolvesToNull() {
        // 未知标签返回 null,由校验层生成"无法解析"阻断问题(禁止静默丢弃)
        assertNull(RosterPositionType.fromLabel("外星岗"));
        assertNull(RosterPositionType.fromLabel(null));
        assertNull(RosterPositionType.fromLabel(""));
    }
}
