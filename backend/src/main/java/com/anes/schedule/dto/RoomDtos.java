package com.anes.schedule.dto;

import jakarta.validation.constraints.NotBlank;

/** 手术室房间 请求/响应对象 */
public class RoomDtos {

    public record RoomCreateReq(@NotBlank(message = "房号不能为空") String code,
                                @NotBlank(message = "区域不能为空") String area,
                                Boolean starred,
                                Boolean schedulable,
                                Integer sort) {
    }

    public record RoomUpdateReq(@NotBlank(message = "房号不能为空") String code,
                                @NotBlank(message = "区域不能为空") String area,
                                Boolean starred,
                                Boolean schedulable,
                                Integer sort,
                                Boolean active) {
    }

    /** 查询参数:area 精确、keyword 模糊匹配房号;分页默认 1/20 */
    public record RoomQuery(String area, String keyword, Long pageNum, Long pageSize) {
        public long page() {
            return pageNum == null || pageNum < 1 ? 1 : pageNum;
        }

        public long size() {
            return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 500);
        }
    }

    public record RoomResp(Long id, String code, String area, Boolean starred,
                           Boolean schedulable, Integer sort, Boolean active) {

        public static RoomResp from(com.anes.schedule.entity.Room r) {
            return new RoomResp(r.getId(), r.getCode(), r.getArea(), r.getStarred(),
                    r.getSchedulable(), r.getSort(), r.getActive());
        }
    }
}
