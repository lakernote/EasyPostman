package com.laker.postman.service.variable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 迭代数据变量提供者。
 * <p>
 * 保存当前执行上下文的 CSV/JSON 行数据，并在同一执行线程内供
 * pm.iterationData 和 {{variable}} 解析复用。
 */
public class IterationDataVariableService implements VariableProvider {

    private static final IterationDataVariableService INSTANCE = new IterationDataVariableService();

    private static final ThreadLocal<Map<String, String>> ITERATION_DATA = new ThreadLocal<>();

    private IterationDataVariableService() {
    }

    public static IterationDataVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData == null) {
            return null;
        }
        return iterationData.get(key);
    }

    @Override
    public boolean has(String key) {
        Map<String, String> iterationData = ITERATION_DATA.get();
        return key != null && iterationData != null && iterationData.containsKey(key);
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData == null || iterationData.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(iterationData));
    }

    @Override
    public int getPriority() {
        return VariableType.ITERATION_DATA.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.ITERATION_DATA;
    }

    public void replaceAll(Map<String, String> variables) {
        Map<String, String> current = getOrCreateContextMap();
        current.clear();
        if (variables != null && !variables.isEmpty()) {
            current.putAll(variables);
        }
    }

    public void remove(String key) {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (key != null && iterationData != null) {
            iterationData.remove(key);
        }
    }

    public void clearValues() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData != null) {
            iterationData.clear();
        }
    }

    /**
     * 彻底解绑当前迭代数据上下文。
     * <p>
     * 由框架层在一次运行结束时调用，避免后续请求复用旧的 CSV/JSON 行数据。
     */
    public void detachContext() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData != null) {
            iterationData.clear();
        }
        ITERATION_DATA.remove();
    }

    public Map<String, String> getCurrentContextMap() {
        return ITERATION_DATA.get();
    }

    public void attachContextMap(Map<String, String> variables) {
        if (variables == null) {
            ITERATION_DATA.remove();
            return;
        }
        ITERATION_DATA.set(variables);
    }

    private Map<String, String> getOrCreateContextMap() {
        Map<String, String> iterationData = ITERATION_DATA.get();
        if (iterationData == null) {
            iterationData = new ConcurrentHashMap<>();
            ITERATION_DATA.set(iterationData);
        }
        return iterationData;
    }
}
