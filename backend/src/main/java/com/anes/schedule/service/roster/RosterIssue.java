package com.anes.schedule.service.roster;

/**
 * 校验问题(design D5):级别 + 行定位(sheet+物理行号) + 消息 + 修正类型。
 * 修正类型是前后端契约:前端按白名单渲染可修正控件,其余只能回 Excel 改后重传。
 */
public record RosterIssue(Level level, String location, String message, FixType fixType) {

    public enum Level {BLOCK, WARN}

    /** 修正类型白名单:人名候选 / 岗位映射 / 日期对齐;NONE = 无系统内修正手段 */
    public enum FixType {STAFF_CANDIDATES, POSITION, DATE, NONE}

    public static RosterIssue block(String location, String message, FixType fixType) {
        return new RosterIssue(Level.BLOCK, location, message, fixType);
    }

    public static RosterIssue warn(String location, String message) {
        return new RosterIssue(Level.WARN, location, message, FixType.NONE);
    }
}
