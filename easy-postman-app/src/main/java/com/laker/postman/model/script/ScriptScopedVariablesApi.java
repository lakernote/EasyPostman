package com.laker.postman.model.script;

import com.laker.postman.model.Environment;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚本中的作用域变量 API。
 * <p>
 * 仅暴露 Postman 风格的变量方法，避免直接把 Environment 模型的内部方法暴露给脚本。
 */
public class ScriptScopedVariablesApi {

    private final Environment scope;
    private final Runnable persistAction;

    public ScriptScopedVariablesApi(Environment scope, Runnable persistAction) {
        this.scope = scope;
        this.persistAction = persistAction;
    }

    public boolean has(String key) {
        return scope != null && scope.has(key);
    }

    public String get(String key) {
        return scope == null ? null : scope.get(key);
    }

    public void set(String key, String value) {
        mutate(() -> scope.set(key, value));
    }

    public void set(String key, Object value) {
        mutate(() -> scope.set(key, value));
    }

    public void unset(String key) {
        mutate(() -> scope.unset(key));
    }

    public void clear() {
        mutate(scope::clear);
    }

    public Object toObject() {
        Map<String, String> values = scope == null ? Collections.emptyMap() : scope.toObject();
        Map<String, Object> jsObject = new LinkedHashMap<>();
        jsObject.putAll(values);
        return ProxyObject.fromMap(jsObject);
    }

    public String replaceIn(String template) {
        return scope == null ? template : scope.replaceIn(template);
    }

    private void mutate(Runnable mutation) {
        if (scope == null) {
            return;
        }
        mutation.run();
        if (persistAction != null) {
            persistAction.run();
        }
    }
}
