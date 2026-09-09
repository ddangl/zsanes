package com.anes.schedule.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 分页结果(total 总数;list 当前页数据) */
@Data
@AllArgsConstructor(staticName = "of")
public class PageResult<T> {

    private long total;
    private List<T> list;
}
