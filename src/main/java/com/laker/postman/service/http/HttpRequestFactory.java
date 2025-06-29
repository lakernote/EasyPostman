package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;

public class HttpRequestFactory {
    public static HttpRequestItem createDefaultRequest() {
        // 创建一个测试请求
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(cn.hutool.core.util.IdUtil.simpleUUID());
        testItem.setName("测试请求");
        testItem.setUrl("https://httpbin.org/get");
        testItem.setMethod("GET");
        // 添加一些默认的请求头
        testItem.getHeaders().put("User-Agent", "EasyPostman HTTP Client");
        testItem.getHeaders().put("Accept", "*/*");
        return testItem;
    }

    public static HttpRequestItem createDefaultWebSocketRequest() {
        // 创建一个测试 WebSocket 请求
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(cn.hutool.core.util.IdUtil.simpleUUID());
        testItem.setName("WebSocket 示例");
        testItem.setUrl("wss://echo.websocket.org");
        testItem.setMethod("GET");
        // 添加一些默认的请求头
        testItem.getHeaders().put("User-Agent", "EasyPostman WebSocket Client");
        return testItem;
    }
}