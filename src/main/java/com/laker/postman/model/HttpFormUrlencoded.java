package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTTP Form Urlencoded model with enabled state
 * 用于 application/x-www-form-urlencoded 类型的表单数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpFormUrlencoded implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";
}
