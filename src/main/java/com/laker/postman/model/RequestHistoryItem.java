package com.laker.postman.model;

/**
 * 请求历史项，包含请求和响应的简要信息
 */
public class RequestHistoryItem {
    public final String method;
    public final String url;
    public final String requestBody;
    public final String requestHeaders;
    public final int responseCode; // 响应状态码
    public final String responseBody;
    public final String responseHeaders;
    public String threadName; // 执行线程名
    public HttpResponse response;

    public RequestHistoryItem(String method, String url, String requestBody, String requestHeaders, String responseHeaders, HttpResponse response) {
        this.method = method;
        this.url = url;
        this.requestBody = requestBody;
        this.requestHeaders = requestHeaders;
        this.responseCode = response.code;
        this.responseHeaders = responseHeaders;
        this.responseBody = response.body;
        this.threadName = response.threadName;
        this.response = response;
    }


    @Override
    public String toString() {
        return String.format("[%s] %s", method, url);
    }
}
