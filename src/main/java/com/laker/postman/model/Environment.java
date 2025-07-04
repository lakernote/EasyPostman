package com.laker.postman.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 环境变量模型，用于管理一组相关的变量
 */
@Data
public class Environment {
    private String id;
    private String name;
    private Map<String, String> variables = new LinkedHashMap<>();
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

    public String get(String key) {
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
    }

    public String getVariable(String key) {
        return variables.get(key);
    }

    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }
}
