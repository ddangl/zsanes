package com.anes.schedule.dto;

import jakarta.validation.constraints.NotBlank;

/** 亚专科 请求/响应对象 */
public class SpecialtyDtos {

    public record SpecialtySaveReq(@NotBlank(message = "名称不能为空") String name,
                                   Integer sort) {
    }

    public record SpecialtyResp(Long id, String name, Integer sort, Boolean active) {
    }
}
