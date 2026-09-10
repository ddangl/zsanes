package com.anes.schedule.dto;

import com.anes.schedule.service.roster.RosterIssue;
import com.anes.schedule.service.roster.RosterRow;

import java.util.List;

/** 月度值班备班表 请求/响应对象(行集合与问题清单与解析域共用) */
public class RosterDtos {

    /** parse 响应:文件月 + 行集合 + 问题清单(适配层+校验器合并)+ 是否存在阻断 */
    public record ParseResp(String fileMonth, List<RosterRow> rows,
                            List<RosterIssue> issues, boolean hasBlock) {
    }

    /** import 响应:月表 id 与明细条数 */
    public record ImportSummary(Long rosterId, int itemCount) {
    }

    /** 当月查询:按日条目 + 顺序池条目 */
    public record MonthResp(Long rosterId, String month, String status,
                            List<DayEntry> byDate, List<PoolEntry> pool) {

        public record DayEntry(String date, String positionType, Long staffId, String staffName) {
        }

        public record PoolEntry(int seq, Long staffId, String staffName) {
        }
    }
}
