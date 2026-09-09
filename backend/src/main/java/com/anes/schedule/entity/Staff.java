package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 人员档案(字段按 2026-08 实况,见 docs/业务规则.md 2.1) */
@Data
@TableName("staff")
public class Staff {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 工号(唯一键,导入匹配以此为准) */
    private String empNo;
    private String name;
    /** 主麻/总值班/本院住院/麻护/规培/进修/轮转/预备 */
    private String jobRole;
    /** 行政职位:主任/副主任(仅主任剔除排班) */
    private String title;
    /** 副麻级别 1~5(8月表无此数据,待确认#3) */
    private Integer subLevel;
    private Long specialty1Id;
    private Long specialty2Id;
    /** 带教上级 id(自关联;导入时由"教师行挂学生工号"转换) */
    private Long mentorId;
    /** 规培年级(1/2/3,8月表未填) */
    private String grade;
    /** 周六是否上班(null 未标注/false 否/true 是) */
    private Boolean saturdayWork;
    /** 周期性缺席,如:每周四金山(待确认#12) */
    private String partTimeRule;
    /** 特殊说明原文 */
    private String note;
    private String phone;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
