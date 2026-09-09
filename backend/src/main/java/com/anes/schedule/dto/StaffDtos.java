package com.anes.schedule.dto;

import jakarta.validation.constraints.NotBlank;

/** 人员档案 请求/响应对象 */
public class StaffDtos {

    /** 查询:keyword 匹配姓名/工号,jobRole 精确,active 过滤;分页默认 1/20 */
    public record StaffQuery(String keyword, String jobRole, Boolean active, Long pageNum, Long pageSize) {
        public long page() {
            return pageNum == null || pageNum < 1 ? 1 : pageNum;
        }

        public long size() {
            return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 500);
        }
    }

    public record StaffSaveReq(String empNo,
                               @NotBlank(message = "姓名不能为空") String name,
                               @NotBlank(message = "职称不能为空") String jobRole,
                               String title,
                               Integer subLevel,
                               Long specialty1Id,
                               Long specialty2Id,
                               Long mentorId,
                               String grade,
                               Boolean saturdayWork,
                               String partTimeRule,
                               String note,
                               String phone,
                               Boolean active) {
    }

    public record StaffResp(Long id, String empNo, String name, String jobRole, String title,
                            Integer subLevel, Long specialty1Id, Long specialty2Id,
                            String specialty1Name, String specialty2Name,
                            Long mentorId, String mentorName,
                            String grade, Boolean saturdayWork, String partTimeRule,
                            String note, String phone, Boolean active) {
    }

    /** 导入轻量项(带教选择下拉等) */
    public record StaffLite(Long id, String empNo, String name, String jobRole) {
    }

    /** 导入结果报告(可变,service 内累加) */
    @lombok.Data
    public static class ImportResult {
        private int totalRows;
        private int inserted;
        private int updated;
        private int skipped;
        private java.util.List<String> warnings = new java.util.ArrayList<>();

        public void warn(String msg) {
            warnings.add(msg);
        }
    }
}
