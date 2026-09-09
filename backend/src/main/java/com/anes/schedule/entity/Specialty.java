package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("specialty")
public class Specialty {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 亚专科名称,如 心外科、普外科 */
    private String name;
    private Integer sort;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
