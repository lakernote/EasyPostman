package com.laker.postman.request.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTTP Header model with enabled state
 */
@Data
@NoArgsConstructor
public class HttpHeader implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";
    private String description = "";

    public HttpHeader(boolean enabled, String key, String value) {
        this(enabled, key, value, "");
    }

    public HttpHeader(boolean enabled, String key, String value, String description) {
        this.enabled = enabled;
        this.key = key;
        this.value = value;
        this.description = description;
    }
}
