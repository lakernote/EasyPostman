package com.laker.postman.service.variable;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 变量上下文提供者
 * <p>
 * 负责管理当前执行上下文内的 pm.variables。
 * <ul>
 *   <li>优先级最高（优先级 = 1）</li>
 *   <li>作用域由调用方决定，可覆盖一次运行或一个虚拟用户线程</li>
 *   <li>使用 ThreadLocal 确保线程安全</li>
 * </ul>
 */
@Slf4j
public class VariablesService implements VariableProvider {

    /**
     * 单例实例
     */
    private static final VariablesService INSTANCE = new VariablesService();

    /**
     * 执行上下文变量存储，使用 ThreadLocal 确保线程安全
     */
    private static final ThreadLocal<Map<String, String>> VARIABLES = new ThreadLocal<>();

    /**
     * 私有构造函数，防止外部实例化
     */
    private VariablesService() {
    }

    /**
     * 获取单例实例
     */
    public static VariablesService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Map<String, String> variables = VARIABLES.get();
        if (variables == null) {
            return null;
        }
        return variables.get(key);
    }

    @Override
    public boolean has(String key) {
        Map<String, String> variables = VARIABLES.get();
        return key != null && variables != null && variables.containsKey(key);
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> variables = VARIABLES.get();
        if (variables == null || variables.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(variables));
    }

    @Override
    public int getPriority() {
        return VariableType.VARIABLE.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.VARIABLE;
    }

    // ==================== 执行上下文变量特有方法 ====================

    /**
     * 设置执行上下文变量
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void set(String key, String value) {
        if (key != null && value != null) {
            getOrCreateContextMap().put(key, value);
            log.debug("Set variable: {} = {}", key, value);
        }
    }

    /**
     * 批量设置执行上下文变量
     *
     * @param variables 变量 Map
     */
    public void setAll(Map<String, String> variables) {
        if (variables != null && !variables.isEmpty()) {
            getOrCreateContextMap().putAll(variables);
            log.debug("Set {} variables", variables.size());
        }
    }

    /**
     * 删除执行上下文变量
     *
     * @param key 变量名
     */
    public void remove(String key) {
        Map<String, String> variables = VARIABLES.get();
        if (key != null && variables != null) {
            variables.remove(key);
            log.debug("Removed variable: {}", key);
        }
    }

    /**
     * 清空当前执行上下文中的变量值，但保留上下文本身。
     * <p>
     * 该方法对应脚本里的 pm.variables.clear() 语义：
     * 后续脚本仍可继续在同一个执行上下文中重新 set 新值。
     */
    public void clearValues() {
        Map<String, String> variables = VARIABLES.get();
        int size = variables != null ? variables.size() : 0;
        if (variables != null) {
            variables.clear();
        }
        log.debug("Cleared {} execution-scoped variables", size);
    }

    /**
     * 彻底解绑当前执行上下文。
     * <p>
     * 该方法只应在一次请求/一次运行结束时由框架层调用，
     * 不应用于脚本内的变量清理。
     */
    public void detachContext() {
        Map<String, String> variables = VARIABLES.get();
        int size = variables != null ? variables.size() : 0;
        if (variables != null) {
            variables.clear();
        }
        VARIABLES.remove();
        log.debug("Detached execution-scoped variable context with {} entries", size);
    }

    /**
     * 获取当前执行上下文变量数量
     *
     * @return 当前执行上下文变量数量
     */
    public int size() {
        Map<String, String> variables = VARIABLES.get();
        return variables != null ? variables.size() : 0;
    }

    public Map<String, String> getCurrentContextMap() {
        return VARIABLES.get();
    }

    public void attachContextMap(Map<String, String> variables) {
        if (variables == null) {
            VARIABLES.remove();
            return;
        }
        VARIABLES.set(variables);
    }

    private Map<String, String> getOrCreateContextMap() {
        Map<String, String> variables = VARIABLES.get();
        if (variables == null) {
            variables = new ConcurrentHashMap<>();
            VARIABLES.set(variables);
        }
        return variables;
    }
}
