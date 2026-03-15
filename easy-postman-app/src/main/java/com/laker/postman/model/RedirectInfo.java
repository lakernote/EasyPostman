package com.laker.postman.model;

import java.util.List;
import java.util.Map;

// 重定向信息结构体
public class RedirectInfo {
    public String url;
    public int statusCode;
    public Map<String, List<String>> headers;
    public String location;
    public String responseBody;
}