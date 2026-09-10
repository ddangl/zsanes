package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("monthly_roster")
public class MonthlyRoster {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 月份 yyyy-MM(原列名 year_month 为 MySQL 保留字,已改名) */
    private String rosterMonth;
    /** DRAFT/ACTIVE(A 方案导入即 ACTIVE) */
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
