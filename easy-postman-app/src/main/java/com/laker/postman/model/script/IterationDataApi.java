package com.laker.postman.model.script;

import cn.hutool.json.JSONUtil;
import com.laker.postman.service.variable.IterationDataVariableService;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 迭代数据 API (pm.iterationData)。
 * <p>
 * 用于只读访问当前执行上下文注入的数据驱动行，不参与持久化。
 */
public class IterationDataApi {

    public boolean has(String key) {
        return IterationDataVariableService.getInstance().has(key);
    }

    public String get(String key) {
        return IterationDataVariableService.getInstance().get(key);
    }

    public Object toObject() {
        Map<String, Object> jsObject = new LinkedHashMap<>();
        jsObject.putAll(IterationDataVariableService.getInstance().getAll());
        return ProxyObject.fromMap(jsObject);
    }

    public String toJSON() {
        return JSONUtil.toJsonStr(new LinkedHashMap<>(IterationDataVariableService.getInstance().getAll()));
    }
}
