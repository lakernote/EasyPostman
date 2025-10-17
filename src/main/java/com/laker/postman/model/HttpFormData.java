package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTTP Form Data model with enabled state
 * 用于 multipart/form-data 类型的表单数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpFormData implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String type = ""; // Text or File
    private String value = "";
}

