package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 规则中心-版本快照:每次发布记录完整 nl_text + payload */
@Data
@TableName("rule_version")
public class RuleVersion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private Integer version;
    private String nlText;
    private String payload;
    private Long publishedBy;
    private LocalDateTime publishedAt;
}
