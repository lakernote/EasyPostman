package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTTP Header model with enabled state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpHeader implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";

}

