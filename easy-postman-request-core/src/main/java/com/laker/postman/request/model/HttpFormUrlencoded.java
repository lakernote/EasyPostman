package com.laker.postman.request.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * HTTP Form Urlencoded model with enabled state
 * 用于 application/x-www-form-urlencoded 类型的表单数据
 */
@Data
@NoArgsConstructor
public class HttpFormUrlencoded implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";
    private String description = "";

    public HttpFormUrlencoded(boolean enabled, String key, String value) {
        this(enabled, key, value, "");
    }

    public HttpFormUrlencoded(boolean enabled, String key, String value, String description) {
        this.enabled = enabled;
        this.key = key;
        this.value = value;
        this.description = description;
    }
}
