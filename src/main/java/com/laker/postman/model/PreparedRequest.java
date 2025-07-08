package com.laker.postman.model;


import okhttp3.Headers;

import java.util.Map;

/**
 * 准备好的请求对象，包含请求的所有必要信息以及替换变量后的内容。
 */
public class PreparedRequest {
    public String id;
    public String url;
    public String method;
    public Map<String, String> headers;
    public Headers okHttpHeaders; // OkHttp 特有的 Headers 对象
    public String body;
    public String okHttpRequestBody; // 真实OkHttp请求体内容
    public Map<String, String> formData;
    public Map<String, String> formFiles;
    // x-www-form-urlencoded 字段
    public Map<String, String> urlencoded;
    public boolean isMultipart;
    public boolean followRedirects = true; // 默认自动重定向
    public boolean logEvent = false; // 默认不记录事件日志

}