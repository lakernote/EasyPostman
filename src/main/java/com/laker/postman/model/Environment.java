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
     * 检查变量是否存在（别名方法）
     * 对应脚本中的: pm.environment.has(key)
     */
    public boolean has(String key) {
        return hasVariable(key);
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

    /**
     * 替换字符串中的变量占位符
     * 将字符串中的 {{variableName}} 格式占位符替换为实际的变量值
     * 同时支持 Postman 动态变量，如 {{$guid}}, {{$timestamp}}, {{$randomInt}} 等
     * 对应脚本中的: pm.environment.replaceIn(template)
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

        // 然后替换环境变量
        if (variableList != null) {
            for (EnvironmentVariable envVar : variableList) {
                if (envVar.isEnabled()) {
                    String placeholder = "{{" + envVar.getKey() + "}}";
                    if (result.contains(placeholder)) {
                        result = result.replace(placeholder, envVar.getValue());
                    }
                }
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

    /**
     * 获取所有变量的对象表示（Postman API）
     * 对应脚本中的: pm.environment.toObject()
     *
     * @return 包含所有已启用变量的 Map（键值对形式）
     */
    public Map<String, String> toObject() {
        return getVariables();
    }
}
