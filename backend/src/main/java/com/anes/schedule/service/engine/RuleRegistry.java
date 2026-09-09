package com.anes.schedule.service.engine;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 规则注册表:Spring 注入全部 RuleHandler,按 ruleType 索引。
 * 新增一种规则处理 = 新增一个 @Component 实现,不改此类。
 */
@Component
public class RuleRegistry {

    private final Map<String, RuleHandler> handlers = new HashMap<>();

    public RuleRegistry(List<RuleHandler> handlerList) {
        handlerList.forEach(h -> handlers.put(h.ruleType(), h));
    }

    public Optional<RuleHandler> find(String ruleType) {
        return Optional.ofNullable(handlers.get(ruleType));
    }

    public Set<String> registeredTypes() {
        return handlers.keySet();
    }
}
