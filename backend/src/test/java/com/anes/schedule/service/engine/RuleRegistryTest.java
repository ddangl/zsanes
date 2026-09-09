package com.anes.schedule.service.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 规则注册表:按 ruleType 索引、未知类型为空 */
class RuleRegistryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private RuleHandler handler(String type) {
        return new RuleHandler() {
            @Override
            public String ruleType() {
                return type;
            }

            @Override
            public void apply(String ruleCode, com.fasterxml.jackson.databind.JsonNode payload,
                              EngineContext ctx) {
                ctx.put(type, ruleCode);
            }
        };
    }

    @Test
    void registersAndFindsByType() {
        RuleRegistry registry = new RuleRegistry(List.of(handler("MANUAL_REMINDER"), handler("FIXED_ASSIGN")));
        assertEquals(2, registry.registeredTypes().size());
        assertTrue(registry.find("MANUAL_REMINDER").isPresent());
        assertTrue(registry.find("FIXED_ASSIGN").isPresent());
        assertTrue(registry.find("UNKNOWN").isEmpty());
    }

    @Test
    void handlerAppliesIntoContext() throws Exception {
        RuleHandler reminder = new ManualReminderHandler();
        EngineContext ctx = new EngineContext();
        reminder.apply("GEN.MANUAL_REMIND_EXPERT_CLINIC",
                mapper.readTree("{\"reminder\":\"专家门诊需手动调整\"}"), ctx);
        assertEquals("专家门诊需手动调整", ctx.get("manualReminder:GEN.MANUAL_REMIND_EXPERT_CLINIC"));
        assertEquals(Map.of("manualReminder:GEN.MANUAL_REMIND_EXPERT_CLINIC", "专家门诊需手动调整").keySet(),
                ctx.all().keySet());
    }
}
