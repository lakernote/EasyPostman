package com.laker.postman.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境变量模型，用于管理一组相关的变量
 */
@Setter
@Getter
public class Environment {
    private String id;
    private String name;
    private List<Variable> variableList = new ArrayList<>();
    private boolean active = false;

    public Environment() {
    }

    public Environment(String name) {
        this.name = name;
    }

    public void addVariable(String key, String value) {
        if (key != null && !key.isEmpty()) {
            Variable variable = new Variable(true, key, value);
            variableList.add(variable);
        }
    }


    public void set(String key, String value) {
        if (key != null && !key.isEmpty()) {
            // 查找是否存在该key的变量
            boolean found = false;
            if (variableList != null) {
                for (Variable envVar : variableList) {
                    if (key.equals(envVar.getKey())) {
                        envVar.setValue(value);
                        envVar.setEnabled(true);
                        found = true;
                        break;
                    }
                }
            }
            // 如果不存在，则添加新变量
            if (!found) {
                if (variableList == null) {
                    variableList = new ArrayList<>();
                }
                variableList.add(new Variable(true, key, value));
            }
        }
    }

    public void removeVariable(String key) {
        if (key != null && variableList != null) {
            variableList.removeIf(envVar -> key.equals(envVar.getKey()));
        }
    }

    public void clearVariables() {
        if (variableList != null) {
            variableList.clear();
        }
    }

    public String get(String key) {
        // 从 variableList 获取已启用的变量
        if (variableList != null && !variableList.isEmpty()) {
            for (Variable envVar : variableList) {
                if (envVar.isEnabled() && key.equals(envVar.getKey())) {
                    return envVar.getValue();
                }
            }
        }
        return null;
    }

    public String getVariable(String key) {
        return get(key);
    }

    public boolean hasVariable(String key) {
        // 从 variableList 查找已启用的变量
        if (variableList != null && !variableList.isEmpty()) {
            for (Variable envVar : variableList) {
                if (envVar.isEnabled() && key.equals(envVar.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取所有变量的 Map 表示
     * 从 variableList 构建 Map，只包含已启用的变量
     */
    public Map<String, String> getVariables() {
        Map<String, String> result = new LinkedHashMap<>();
        if (variableList != null) {
            for (Variable envVar : variableList) {
                if (envVar.isEnabled()) {
                    result.put(envVar.getKey(), envVar.getValue());
                }
            }
        }
        return result;
    }
}
