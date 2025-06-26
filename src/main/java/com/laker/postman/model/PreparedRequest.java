package com.laker.postman.model;

import java.util.Map;

/**
 * 准备好的请求对象，包含请求的所有必要信息以及替换变量后的内容。
 */
public class PreparedRequest {
    public String url;
    public String method;
    public Map<String, String> headers;
    public String body;
    public Map<String, String> formData;
    public Map<String, String> formFiles;
    public boolean isMultipart;
    public boolean followRedirects = true; // 默认自动重定向
}