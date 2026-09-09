package com.anes.schedule.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 人员档案 Excel 导入行(与科室 8 月表 13 列一一对应,按列下标映射;
 * 第 3 列年龄全空,不映射)。同时用于生成导入模板。
 */
@Data
public class StaffImportRow {

    @ColumnWidth(12)
    @ExcelProperty(value = "工号", index = 0)
    private String empNo;

    @ColumnWidth(12)
    @ExcelProperty(value = "姓名", index = 1)
    private String name;

    @ColumnWidth(14)
    @ExcelProperty(value = "职位(主任/副主任)", index = 3)
    private String title;

    @ColumnWidth(18)
    @ExcelProperty(value = "职称(主麻、总值班、本院住院、麻护、规培、进修、轮转)", index = 4)
    private String jobRole;

    @ColumnWidth(14)
    @ExcelProperty(value = "第一亚专科", index = 5)
    private String specialty1Name;

    @ColumnWidth(14)
    @ExcelProperty(value = "第二亚专科", index = 6)
    private String specialty2Name;

    @ColumnWidth(16)
    @ExcelProperty(value = "带教学生一工号", index = 7)
    private String mentor1EmpNo;

    @ColumnWidth(16)
    @ExcelProperty(value = "带教学生二工号", index = 8)
    private String mentor2EmpNo;

    @ColumnWidth(16)
    @ExcelProperty(value = "带教学生三工号", index = 9)
    private String mentor3EmpNo;

    @ColumnWidth(12)
    @ExcelProperty(value = "规培年级", index = 10)
    private String grade;

    @ColumnWidth(14)
    @ExcelProperty(value = "周六是否上班", index = 11)
    private String saturdayWork;

    @ColumnWidth(40)
    @ExcelProperty(value = "特殊说明", index = 12)
    private String note;
}
