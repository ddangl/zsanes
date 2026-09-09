package com.anes.schedule.dto;

import jakarta.validation.constraints.NotBlank;

/** 规则中心 请求/响应对象 */
public class RuleDtos {

    public record RuleQuery(String category, String status, String keyword,
                            Long pageNum, Long pageSize) {
        public long page() {
            return pageNum == null || pageNum < 1 ? 1 : pageNum;
        }

        public long size() {
            return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        }
    }

    public record RuleSaveReq(String ruleCode,
                              @NotBlank(message = "名称不能为空") String name,
                              @NotBlank(message = "分类不能为空") String category,
                              @NotBlank(message = "自然语言原文不能为空") String nlText,
                              @NotBlank(message = "payload 不能为空") String payload,
                              String ruleType,
                              Integer priority,
                              String effectiveFrom,
                              String effectiveTo) {
    }

    public record RuleResp(Long id, String ruleCode, String category, String name, String nlText,
                           String payload, String ruleType, String status, Integer priority,
                           Integer version) {
    }

    public record VersionResp(Integer version, String nlText, String payload,
                              Long publishedBy, String publishedAt) {
    }
}
