package com.laker.postman.service.variable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 显式脚本执行上下文。
 * <p>
 * 将一次请求/一次运行需要共享的 execution variables 与 iteration data
 * 从线程本身解耦，方便在跨线程回调场景中显式复用。
 */
public class ExecutionVariableContext {

    private final Map<String, String> variables;
    private final Map<String, String> iterationData;

    public ExecutionVariableContext() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public ExecutionVariableContext(Map<String, String> variables, Map<String, String> iterationData) {
        this.variables = variables != null ? variables : new ConcurrentHashMap<>();
        this.iterationData = iterationData != null ? iterationData : new ConcurrentHashMap<>();
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, String> getIterationData() {
        return iterationData;
    }

    public void replaceIterationData(Map<String, String> values) {
        iterationData.clear();
        if (values != null && !values.isEmpty()) {
            iterationData.putAll(values);
        }
    }
}
