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
    private List<EnvironmentVariable> variableList = new ArrayList<>();
    private boolean active = false;

    public Environment() {
    }

    public Environment(String name) {
        this.name = name;
    }

    public void addVariable(String key, String value) {
        if (key != null && !key.isEmpty()) {
            EnvironmentVariable variable = new EnvironmentVariable(true, key, value);
            variableList.add(variable);
        }
    }


    public void set(String key, String value) {
        if (key != null && !key.isEmpty()) {
            // 查找是否存在该key的变量
            boolean found = false;
            if (variableList != null) {
                for (EnvironmentVariable envVar : variableList) {
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
                variableList.add(new EnvironmentVariable(true, key, value));
            }
        }
    }



    /**
     * 重载set方法，支持任意Object类型参数，自动转换为String
     * 解决JavaScript中传入数字等非String类型的问题
     */
    public void set(String key, Object value) {
        if (key != null && !key.isEmpty() && value != null) {
            String strValue = String.valueOf(value);
            set(key, strValue);
        }
    }

    public void removeVariable(String key) {
        if (key != null && variableList != null) {
            variableList.removeIf(envVar -> key.equals(envVar.getKey()));
        }
    }


    public String get(String key) {
        // 从 variableList 获取已启用的变量
        if (variableList != null && !variableList.isEmpty()) {
            for (EnvironmentVariable envVar : variableList) {
                if (envVar.isEnabled() && key.equals(envVar.getKey())) {
                    return envVar.getValue();
                }
            }
        }
        return null;
    }

    // for javascript
    public void unset(String key) {
        if (key != null && variableList != null) {
            variableList.removeIf(envVar -> key.equals(envVar.getKey()));
        }
    }

    // for javascript
    public void clear() {
        if (variableList != null) {
            variableList.clear();
        }
    }

    public String getVariable(String key) {
        return get(key);
    }

    // for javascript
    public boolean hasVariable(String key) {
        // 从 variableList 查找已启用的变量
        if (variableList != null && !variableList.isEmpty()) {
            for (EnvironmentVariable envVar : variableList) {
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
            for (EnvironmentVariable envVar : variableList) {
                if (envVar.isEnabled()) {
                    result.put(envVar.getKey(), envVar.getValue());
                }
            }
        }
        return result;
    }
}
