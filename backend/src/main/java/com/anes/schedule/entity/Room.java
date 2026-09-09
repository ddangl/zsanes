package com.anes.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 房间号,如 22、802 */
    private String code;
    /** 区域,如 8号楼4楼手术室 */
    private String area;
    /** 星标术间(信息源暂缺,一期无数据) */
    private Boolean starred;
    /** 是否参与本科排班:0=眼科(无全麻需求)/心外科(单独管理) */
    private Boolean schedulable;
    private Integer sort;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
