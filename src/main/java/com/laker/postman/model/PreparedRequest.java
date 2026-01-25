package com.laker.postman.model;


import okhttp3.Headers;

import java.util.List;

/**
 * 准备好的请求对象，包含请求的所有必要信息以及替换变量后的内容。
 */
public class PreparedRequest {
    public String id;
    public String url;
    public String method;

    public String body;
    public String bodyType;

    public List<HttpHeader> headersList;
    public List<HttpFormData> formDataList;
    public List<HttpFormUrlencoded> urlencodedList;
    public List<HttpParam> paramsList;

    public boolean isMultipart;
    public boolean followRedirects = true; // 默认自动重定向
    public boolean logEvent = false; // 默认不记录事件日志

    public Headers okHttpHeaders; // OkHttp 特有的 Headers 对象
    public String okHttpRequestBody; // 真实OkHttp请求体内容

    /**
     * 创建当前对象的浅拷贝
     * 注意：List 和 Map 对象本身不会被深拷贝，只是引用复制
     */
    public PreparedRequest shallowCopy() {
        PreparedRequest copy = new PreparedRequest();
        copy.id = this.id;
        copy.url = this.url;
        copy.method = this.method;
        copy.okHttpHeaders = this.okHttpHeaders;
        copy.body = this.body;
        copy.bodyType = this.bodyType;
        copy.okHttpRequestBody = this.okHttpRequestBody;
        copy.isMultipart = this.isMultipart;
        copy.followRedirects = this.followRedirects;
        copy.logEvent = this.logEvent;
        copy.headersList = this.headersList;
        copy.formDataList = this.formDataList;
        copy.urlencodedList = this.urlencodedList;
        copy.paramsList = this.paramsList;
        return copy;
    }

    /**
     * 简化对象，将渲染时不需要的字段置为 null，减少内存占用
     * 保留的字段：url, method, okHttpHeaders, formDataList, urlencodedList, okHttpRequestBody
     * 置为 null 的字段：id, body, bodyType, headersList, paramsList
     */
    public void simplify() {
        this.id = null;
        this.body = null;
        this.bodyType = null;
        this.headersList = null;  // 渲染用的是 okHttpHeaders
        this.paramsList = null;   // 渲染时不显示
        // isMultipart, followRedirects, logEvent 是基本类型，不占内存
    }
}