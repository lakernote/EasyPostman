package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Form-Data Parameter model with enabled state
 * Supports both text and file types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpFormData implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String type = "Text"; // "Text" or "File"
    private String value = "";
}

