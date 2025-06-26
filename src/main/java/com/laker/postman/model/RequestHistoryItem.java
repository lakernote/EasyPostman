package com.laker.postman.model;

/**
 * 请求历史项，包含请求和响应的简要信息
 */
public class RequestHistoryItem {
    public final String method;
    public final String url;
    public final int responseCode; // 响应状态码
    public PreparedRequest request; // 原始请求对象
    public HttpResponse response; // 响应对象

    public RequestHistoryItem(PreparedRequest request, HttpResponse response) {
        this.method = request.method;
        this.url = request.url;
        this.responseCode = response.code;
        this.request = request;
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", method, url);
    }
}