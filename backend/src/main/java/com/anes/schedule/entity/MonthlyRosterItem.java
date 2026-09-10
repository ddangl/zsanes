package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 月表明细:duty_date=当日固定安排;null+seq=顺序池(短接班按序顺延) */
@Data
@TableName("monthly_roster_item")
public class MonthlyRosterItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rosterId;
    private LocalDate dutyDate;
    private String positionType;
    private Integer seq;
    private Long staffId;
}
