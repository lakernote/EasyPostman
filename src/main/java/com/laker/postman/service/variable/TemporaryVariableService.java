package com.laker.postman.service.variable;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临时变量提供者
 * <p>
 * 负责管理请求生命周期内的临时变量
 * <ul>
 *   <li>优先级最高（优先级 = 1）</li>
 *   <li>仅在当前请求执行过程中有效</li>
 *   <li>使用 ThreadLocal 确保线程安全</li>
 * </ul>
 */
@Slf4j
public class TemporaryVariableService implements VariableProvider {

    /**
     * 单例实例
     */
    private static final TemporaryVariableService INSTANCE = new TemporaryVariableService();

    /**
     * 临时变量存储，使用 ThreadLocal 确保线程安全
     */
    private static final ThreadLocal<Map<String, String>> TEMPORARY_VARIABLES =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * 私有构造函数，防止外部实例化
     */
    private TemporaryVariableService() {
    }

    /**
     * 获取单例实例
     */
    public static TemporaryVariableService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return TEMPORARY_VARIABLES.get().get(key);
    }

    @Override
    public boolean has(String key) {
        return key != null && TEMPORARY_VARIABLES.get().containsKey(key);
    }

    @Override
    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(TEMPORARY_VARIABLES.get()));
    }

    @Override
    public int getPriority() {
        return 1; // 最高优先级
    }

    @Override
    public String getName() {
        return "Temporary Variables";
    }

    // ==================== 临时变量特有方法 ====================

    // ==================== 临时变量特有方法 ====================

    /**
     * 设置临时变量
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void set(String key, String value) {
        if (key != null && value != null) {
            TEMPORARY_VARIABLES.get().put(key, value);
            log.debug("Set temporary variable: {} = {}", key, value);
        }
    }

    /**
     * 批量设置临时变量（用于同步 pm.variables）
     *
     * @param variables 变量 Map
     */
    public void setAll(Map<String, String> variables) {
        if (variables != null && !variables.isEmpty()) {
            TEMPORARY_VARIABLES.get().putAll(variables);
            log.debug("Set {} temporary variables", variables.size());
        }
    }

    /**
     * 删除临时变量
     *
     * @param key 变量名
     */
    public void remove(String key) {
        if (key != null) {
            TEMPORARY_VARIABLES.get().remove(key);
            log.debug("Removed temporary variable: {}", key);
        }
    }

    /**
     * 清空所有临时变量
     */
    public void clear() {
        int size = TEMPORARY_VARIABLES.get().size();
        TEMPORARY_VARIABLES.get().clear();
        TEMPORARY_VARIABLES.remove();
        log.debug("Cleared {} temporary variables", size);
    }

    /**
     * 获取临时变量数量
     *
     * @return 临时变量数量
     */
    public int size() {
        return TEMPORARY_VARIABLES.get().size();
    }
}
