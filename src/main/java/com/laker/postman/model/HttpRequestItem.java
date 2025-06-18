package com.laker.postman.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HttpRequestItem 类表示一个HTTP请求的配置项
 * 包含请求的基本信息、头部、参数、认证等
 * 每个参数都要求有默认值，便于前端展示和编辑
 */
@Data
public class HttpRequestItem implements Serializable {
    private String id = ""; // 唯一标识符
    private String name = ""; // 请求名称
    private String url = ""; // 请求URL
    private String method = "GET"; // 请求方法（GET, POST, PUT, DELETE等）
    private Map<String, String> headers = new LinkedHashMap<>(); // 请求头
    private String body = ""; // 请求体内容（如JSON、表单数据等）
    private Map<String, String> params = new LinkedHashMap<>(); // 请求参数（查询字符串）

    // form-data 普通字段
    private Map<String, String> formData = new LinkedHashMap<>();
    // form-data 文件字段（key=字段名，value=文件绝对路径）
    private Map<String, String> formFiles = new LinkedHashMap<>();

    // 变量提取规则列表，存储为[变量名, JSON路径]的对象列表
    private List<ExtractorRule> extractorRules = new ArrayList<>();

    // 是否自动提取变量（默认开启）
    private boolean autoExtractVariables = true;

    // 认证相关字段
    private String authType = "none"; // 认证类型（none/basic/bearer）
    private String authUsername = ""; // Basic用户名
    private String authPassword = ""; // Basic密码
    private String authToken = "";    // Bearer Token

    // 前置脚本（请求前执行）
    private String prescript = "";
    // 后置脚本（响应后执行）
    private String postscript = "";

    // 是否自动重定向（默认true）
    public Boolean isFollowRedirects = true;

    /**
     * 变量提取规则类
     */
    @Data
    public static class ExtractorRule implements Serializable {
        private String variableName = ""; // 变量名
        private String jsonPath = "";     // JSON路径

        public ExtractorRule(String variableName, String jsonPath) {
            this.variableName = variableName;
            this.jsonPath = jsonPath;
        }
    }
}