package com.laker.postman.request.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTTP Parameter model with enabled state
 */
@Data
@NoArgsConstructor
public class HttpParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";
    private String description = "";

    public HttpParam(boolean enabled, String key, String value) {
        this(enabled, key, value, "");
    }

    public HttpParam(boolean enabled, String key, String value, String description) {
        this.enabled = enabled;
        this.key = key;
        this.value = value;
        this.description = description;
    }
}
