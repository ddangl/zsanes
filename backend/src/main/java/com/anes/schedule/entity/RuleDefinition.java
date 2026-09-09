package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 规则中心-规则定义:nl_text(科室原话)+ payload(结构化参数)双形态,见实施规划 4.7 */
@Data
@TableName("rule_definition")
public class RuleDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 规则编码,如 OR.FIXED_ASSIGN_LU */
    private String ruleCode;
    /** DUTY/OR/STAFF_AVAILABILITY/PERIPHERAL/LEAVE/GENERAL */
    private String category;
    private String name;
    /** 科室原话(自然语言原文,存档溯源) */
    private String nlText;
    /** 结构化参数 JSON 字符串(引擎执行) */
    private String payload;
    /** 引擎挂载类型,对应 RuleHandler.ruleType() */
    private String ruleType;
    /** DRAFT/ACTIVE/RETIRED */
    private String status;
    private Integer priority;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    /** 已发布版本号 */
    private Integer version;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
