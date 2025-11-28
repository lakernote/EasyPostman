package com.laker.postman.model.script;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 临时变量 API (pm.variables)
 * <p>
 * 用于存储请求生命周期内的临时变量，这些变量不会被持久化，
 * 只在当前请求执行过程中有效。支持从 CSV 数据驱动测试等场景注入数据。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>存储中间计算结果</li>
 *   <li>在前置和后置脚本之间传递数据</li>
 *   <li>访问 CSV 数据驱动测试的当前行数据</li>
 * </ul>
 */
public class TemporaryVariablesApi {
    /**
     * 变量存储 Map
     */
    private final Map<String, String> variablesMap = new LinkedHashMap<>();

    /**
     * 设置临时变量
     *
     * @param key   变量名
     * @param value 变量值
     */
    public void set(String key, String value) {
        if (key != null && value != null) {
            variablesMap.put(key, value);
        }
    }

    /**
     * 获取临时变量
     *
     * @param key 变量名
     * @return 变量值，不存在则返回 null
     */
    public String get(String key) {
        return variablesMap.get(key);
    }

    /**
     * 检查变量是否存在
     *
     * @param key 变量名
     * @return 是否存在
     */
    public boolean has(String key) {
        return variablesMap.containsKey(key);
    }

    /**
     * 删除临时变量
     *
     * @param key 变量名
     */
    public void unset(String key) {
        variablesMap.remove(key);
    }

    /**
     * 清空所有临时变量
     */
    public void clear() {
        variablesMap.clear();
    }

    /**
     * 批量设置临时变量（用于 CSV 数据注入）
     *
     * @param variables 变量 Map
     */
    public void replaceAll(Map<String, String> variables) {
        if (variables != null) {
            variablesMap.clear();
            variablesMap.putAll(variables);
        }
    }

    /**
     * 获取所有临时变量
     *
     * @return 变量 Map 的副本
     */
    public Map<String, String> toObject() {
        return new java.util.LinkedHashMap<>(variablesMap);
    }
}