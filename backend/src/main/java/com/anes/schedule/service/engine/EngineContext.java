package com.anes.schedule.service.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * 排班引擎上下文(占位):一期仅承载规则处理器写入的提示信息;
 * 实施步骤 4/5(排班管道)扩展为完整上下文(人员池、当日分配等)。
 */
public class EngineContext {

    private final Map<String, Object> attrs = new HashMap<>();

    public void put(String key, Object value) {
        attrs.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) attrs.get(key);
    }

    public Map<String, Object> all() {
        return attrs;
    }
}
