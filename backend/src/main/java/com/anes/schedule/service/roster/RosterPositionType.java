package com.anes.schedule.service.roster;

import java.util.Map;

/**
 * 月度值班备班表 10 类岗位(specs/monthly-roster「岗位落库形态」)。
 * 中文标签映射为解析适配层的常量假设,真实样本到位后在此扩充(含测试)。
 */
public enum RosterPositionType {

    ON_CALL_CHIEF(RosterForm.BY_DATE),
    ON_CALL_T2(RosterForm.BY_DATE),
    ON_CALL_T3(RosterForm.BY_DATE),
    ON_CALL_T4(RosterForm.BY_DATE),
    STANDBY1(RosterForm.BY_DATE),
    STANDBY2(RosterForm.BY_DATE),
    SHORT_RELIEF(RosterForm.SEQUENTIAL_POOL),
    LONG_RELIEF(RosterForm.BY_DATE),
    ASSISTANT_RELIEF(RosterForm.BY_DATE),
    ASSISTANT_MID(RosterForm.BY_DATE);

    private static final Map<String, RosterPositionType> LABELS = Map.ofEntries(
            Map.entry("值班老总", ON_CALL_CHIEF),
            Map.entry("老总", ON_CALL_CHIEF),
            Map.entry("二档", ON_CALL_T2),
            Map.entry("三档", ON_CALL_T3),
            Map.entry("四档", ON_CALL_T4),
            Map.entry("备班1", STANDBY1),
            Map.entry("备班2", STANDBY2),
            Map.entry("短接班", SHORT_RELIEF),
            Map.entry("长接班", LONG_RELIEF),
            Map.entry("副麻接班", ASSISTANT_RELIEF),
            Map.entry("副麻中班", ASSISTANT_MID));

    private final RosterForm form;

    RosterPositionType(RosterForm form) {
        this.form = form;
    }

    public RosterForm form() {
        return form;
    }

    /** 未知/空白标签返回 null,由校验层生成"无法解析"阻断问题,禁止静默丢弃 */
    public static RosterPositionType fromLabel(String label) {
        if (label == null) {
            return null;
        }
        return LABELS.get(label.trim());
    }
}
