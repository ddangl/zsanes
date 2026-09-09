package com.anes.schedule.service.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 示例规则处理器:MANUAL_REMINDER(未结构化规则)。
 * 一期:提醒文本进上下文,工作台展示提醒卡(步骤 5 接线)。
 */
@Component
public class ManualReminderHandler implements RuleHandler {

    @Override
    public String ruleType() {
        return "MANUAL_REMINDER";
    }

    @Override
    public void apply(String ruleCode, JsonNode payload, EngineContext ctx) {
        String reminder = payload.path("reminder").asText("");
        if (!reminder.isEmpty()) {
            ctx.put("manualReminder:" + ruleCode, reminder);
        }
    }
}
