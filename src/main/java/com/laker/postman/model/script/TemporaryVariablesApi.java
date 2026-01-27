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
     * 设置临时变量（支持任意类型，自动转换为字符串）
     * 解决JavaScript中传入数字等非String类型的问题
     *
     * @param key   变量名
     * @param value 变量值（任意类型）
     */
    public void set(String key, Object value) {
        if (key != null && value != null) {
            variablesMap.put(key, String.valueOf(value));
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

    /**
     * 替换字符串中的变量占位符
     * 将字符串中的 {{variableName}} 格式占位符替换为实际的变量值
     * 同时支持 Postman 动态变量，如 {{$guid}}, {{$timestamp}}, {{$randomInt}} 等
     *
     * @param template 包含变量占位符的模板字符串
     * @return 替换后的字符串
     */
    public String replaceIn(String template) {
        if (template == null) {
            return null;
        }

        String result = template;

        // 首先替换 Postman 动态变量
        result = replaceDynamicVariables(result);

        // 然后替换用户定义的临时变量
        for (Map.Entry<String, String> entry : variablesMap.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue());
            }
        }

        return result;
    }

    /**
     * 替换 Postman 动态变量
     */
    private String replaceDynamicVariables(String template) {
        String result = template;

        // {{$guid}} - 生成 UUID
        if (result.contains("{{$guid}}")) {
            result = result.replace("{{$guid}}", java.util.UUID.randomUUID().toString());
        }

        // {{$timestamp}} - 当前时间戳（秒）
        if (result.contains("{{$timestamp}}")) {
            result = result.replace("{{$timestamp}}", String.valueOf(System.currentTimeMillis() / 1000));
        }

        // {{$randomInt}} - 随机整数 (0-1000)
        if (result.contains("{{$randomInt}}")) {
            result = result.replace("{{$randomInt}}", String.valueOf(new java.util.Random().nextInt(1001)));
        }

        // {{$randomUUID}} - 生成 UUID (别名)
        if (result.contains("{{$randomUUID}}")) {
            result = result.replace("{{$randomUUID}}", java.util.UUID.randomUUID().toString());
        }

        // {{$isoTimestamp}} - ISO 8601 格式时间戳
        if (result.contains("{{$isoTimestamp}}")) {
            result = result.replace("{{$isoTimestamp}}", java.time.Instant.now().toString());
        }

        return result;
    }
}