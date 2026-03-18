package com.laker.postman.model;

import java.util.List;

/**
 * 临时数据结构，用于解析 curl 命令
 */
public class CurlRequest {
    public String url;
    public String method;
    public List<HttpHeader> headersList;
    public String body;
    public List<HttpParam> paramsList;
    // 用于存储解析出的表单数据 (multipart/form-data)
    public List<HttpFormData> formDataList;
    // 用于存储 application/x-www-form-urlencoded 类型的数据
    public List<HttpFormUrlencoded> urlencodedList;
    public boolean followRedirects = false; // 是否跟随重定向
}