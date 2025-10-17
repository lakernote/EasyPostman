package com.laker.postman.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境变量模型，用于管理一组相关的变量
 */
@Data
public class Environment {
    private String id;
    private String name;
    private Map<String, String> variables = new LinkedHashMap<>();
    private List<EnvironmentVariable> variableList = new ArrayList<>();
    private boolean active = false;

    public Environment() {
    }

    public Environment(String name) {
        this.name = name;
    }

    public void addVariable(String key, String value) {
        if (key != null && !key.isEmpty()) {
            variables.put(key, value);
        }
    }

    public void removeVariable(String key) {
        if (key != null) {
            variables.remove(key);
        }
    }

    public void set(String key, String value) {
        if (key != null && !key.isEmpty()) {
            variables.put(key, value);
        }
    }

    /**
     * 重载set方法，支持任意Object类型参数，自动转换为String
     * 解决JavaScript中传入数字等非String类型的问题
     */
    public void set(String key, Object value) {
        if (key != null && !key.isEmpty() && value != null) {
            variables.put(key, String.valueOf(value));
        }
    }

    public String get(String key) {
        // 优先从新格式获取已启用的变量
        if (variableList != null && !variableList.isEmpty()) {
            for (EnvironmentVariable var : variableList) {
                if (var.isEnabled() && key.equals(var.getKey())) {
                    return var.getValue();
                }
            }
        }
        // 后备方案：从旧格式获取
        return variables.get(key);
    }

    // for javascript
    public void unset(String key) {
        if (key != null) {
            variables.remove(key);
        }
    }

    // for javascript
    public void clear() {
        variables.clear();
        if (variableList != null) {
            variableList.clear();
        }
    }

    public String getVariable(String key) {
        return get(key);
    }

    public boolean hasVariable(String key) {
        // 优先从新格式查找已启用的变量
        if (variableList != null && !variableList.isEmpty()) {
            for (EnvironmentVariable var : variableList) {
                if (var.isEnabled() && key.equals(var.getKey())) {
                    return true;
                }
            }
        }
        // 后备方案：从旧格式查找
        return variables.containsKey(key);
    }

    /**
     * 从旧格式迁移到新格式
     * 用于数据兼容性
     */
    public void migrateToNewFormat() {
        if ((variableList == null || variableList.isEmpty()) && variables != null && !variables.isEmpty()) {
            variableList = new ArrayList<>();
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                variableList.add(new EnvironmentVariable(true, entry.getKey(), entry.getValue()));
            }
        }
    }
}
