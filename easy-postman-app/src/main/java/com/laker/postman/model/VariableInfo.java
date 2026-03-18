package com.laker.postman.model;

import com.laker.postman.service.variable.VariableType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 变量信息
 * <p>
 * 用于在UI中展示变量的完整信息（名称、值、类型）
 */
@Data
@AllArgsConstructor
public class VariableInfo {
    /**
     * 变量名
     */
    private String name;

    /**
     * 变量值
     */
    private String value;

    /**
     * 变量类型
     */
    private VariableType type;
}
