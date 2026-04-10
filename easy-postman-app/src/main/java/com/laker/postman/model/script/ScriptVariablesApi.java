package com.laker.postman.model.script;

import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.variable.EnvironmentVariableService;
import com.laker.postman.service.variable.GroupVariableService;
import com.laker.postman.service.variable.VariableResolver;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚本中的局部变量 API。
 * <p>
 * set/unset/clear 仅影响当前脚本执行周期内的局部变量。
 * get/has/toObject/replaceIn 按应用内变量优先级读取，行为与 {{variable}} 解析保持一致。
 */
public class ScriptVariablesApi {

    private final TemporaryVariablesApi localVariables = new TemporaryVariablesApi();

    public void set(String key, String value) {
        localVariables.set(key, value);
    }

    public void set(String key, Object value) {
        localVariables.set(key, value);
    }

    public String get(String key) {
        String localValue = localVariables.get(key);
        if (localValue != null) {
            return localValue;
        }

        String groupValue = GroupVariableService.getInstance().get(key);
        if (groupValue != null) {
            return groupValue;
        }

        String envValue = EnvironmentVariableService.getInstance().get(key);
        if (envValue != null) {
            return envValue;
        }

        return GlobalVariablesService.getInstance().get(key);
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    public void unset(String key) {
        localVariables.unset(key);
    }

    public void clear() {
        localVariables.clear();
    }

    public Object toObject() {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.putAll(GlobalVariablesService.getInstance().getAll());
        merged.putAll(EnvironmentVariableService.getInstance().getAll());
        merged.putAll(GroupVariableService.getInstance().getAll());
        merged.putAll(localVariables.toObject());
        Map<String, Object> jsObject = new LinkedHashMap<>();
        jsObject.putAll(merged);
        return ProxyObject.fromMap(jsObject);
    }

    public String replaceIn(String template) {
        String resolvedLocal = localVariables.replaceIn(template);
        return VariableResolver.resolve(resolvedLocal);
    }

    public Map<String, String> toLocalObject() {
        return localVariables.toObject();
    }
}
