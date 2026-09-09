package com.anes.schedule.service.engine;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 规则处理器 SPI:每种 rule_type 一个实现,Spring 自动收集进 RuleRegistry。
 * 引擎(实施步骤 4/5)按挂载点取 ACTIVE 规则,交给对应 Handler 解释 payload。
 */
public interface RuleHandler {

    /** 对应 rule_definition.rule_type */
    String ruleType();

    /** 处理一条规则:解释 payload,作用于引擎上下文 */
    void apply(String ruleCode, JsonNode payload, EngineContext ctx);
}
