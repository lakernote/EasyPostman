package com.laker.postman.service.js.api;

import com.laker.postman.service.variable.VariableResolver;
import com.laker.postman.service.variable.VariablesService;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.variable.EnvironmentVariableService;
import com.laker.postman.service.variable.GroupVariableService;
import com.laker.postman.service.variable.IterationDataVariableService;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚本中的 pm.variables API。
 * <p>
 * set/unset/clear 作用于当前执行上下文变量。
 * get/has/toObject/replaceIn 按应用内变量优先级读取，行为与 {{variable}} 解析保持一致。
 */
public class ScriptVariablesApi {
    private final boolean includeIterationData;

    public ScriptVariablesApi() {
        this(false);
    }

    private ScriptVariablesApi(boolean includeIterationData) {
        this.includeIterationData = includeIterationData;
    }

    static ScriptVariablesApi withIterationData() {
        return new ScriptVariablesApi(true);
    }

    public void set(String key, String value) {
        VariablesService.getInstance().set(key, value);
    }

    public void set(String key, Object value) {
        if (key != null && value != null) {
            VariablesService.getInstance().set(key, String.valueOf(value));
        }
    }

    public String get(String key) {
        return resolveScriptVariable(key);
    }

    public boolean has(String key) {
        return resolveScriptVariable(key) != null;
    }

    public void unset(String key) {
        VariablesService.getInstance().remove(key);
    }

    public void clear() {
        VariablesService.getInstance().clearValues();
    }

    public Object toObject() {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.putAll(GlobalVariablesService.getInstance().getAll());
        merged.putAll(EnvironmentVariableService.getInstance().getAll());
        merged.putAll(GroupVariableService.getInstance().getAll());
        if (includeIterationData) {
            merged.putAll(IterationDataVariableService.getInstance().getAll());
        }
        merged.putAll(VariablesService.getInstance().getAll());
        Map<String, Object> jsObject = new LinkedHashMap<>();
        jsObject.putAll(merged);
        return ProxyObject.fromMap(jsObject);
    }

    public String replaceIn(String template) {
        return VariableResolver.resolve(template);
    }

    private String resolveScriptVariable(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String value = VariablesService.getInstance().get(key);
        if (value != null) {
            return value;
        }

        if (includeIterationData) {
            value = IterationDataVariableService.getInstance().get(key);
            if (value != null) {
                return value;
            }
        }

        value = GroupVariableService.getInstance().get(key);
        if (value != null) {
            return value;
        }

        value = EnvironmentVariableService.getInstance().get(key);
        if (value != null) {
            return value;
        }

        return GlobalVariablesService.getInstance().get(key);
    }
}
