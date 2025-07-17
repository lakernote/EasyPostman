package com.laker.postman.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用 Map 包装类，支持 JS add 方法
 */
public class ParamMapWrapper extends LinkedHashMap<String, String> {
    private final Map<String, String> delegate;
    private boolean isParams;
    private JsRequestWrapper jsRequestWrapper;

    public ParamMapWrapper(Map<String, String> delegate) {
        super(delegate);
        this.delegate = delegate;
        this.isParams = false;
    }

    // 用于 params 字段，自动同步 url
    public ParamMapWrapper(Map<String, String> delegate, JsRequestWrapper jsRequestWrapper) {
        super(delegate);
        this.delegate = delegate;
        this.isParams = true;
        this.jsRequestWrapper = jsRequestWrapper;
    }

    public void add(Map<String, Object> obj) {
        if (obj != null) {
            Object k = obj.get("key");
            Object v = obj.get("value");
            if (k != null && v != null) {
                delegate.put(String.valueOf(k), String.valueOf(v));
                if (isParams && jsRequestWrapper != null) {
                    jsRequestWrapper.updateUrlWithParams(delegate);
                }
                // urlencoded字段变更时自动同步body
                if (jsRequestWrapper != null && jsRequestWrapper.urlencoded == this) {
                    jsRequestWrapper.updateBodyWithUrlencoded();
                }
            }
        }
    }

    public void add(String key, String value) {
        if (key != null && value != null) {
            delegate.put(key, value);
            if (isParams && jsRequestWrapper != null) {
                jsRequestWrapper.updateUrlWithParams(delegate);
            }
            // urlencoded字段变更时自动同步body
            if (jsRequestWrapper != null && jsRequestWrapper.urlencoded == this) {
                jsRequestWrapper.updateBodyWithUrlencoded();
            }
        }
    }
}
