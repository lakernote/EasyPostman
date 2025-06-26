package com.laker.postman.model;

import java.util.Map;

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