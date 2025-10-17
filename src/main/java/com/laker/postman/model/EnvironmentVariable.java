package com.laker.postman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Environment Variable model with enabled state
 * 用于环境变量数据，支持启用/禁用状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentVariable implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled = true;
    private String key = "";
    private String value = "";
}

