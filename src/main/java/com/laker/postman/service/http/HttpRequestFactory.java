package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;

public class HttpRequestFactory {
    public static HttpRequestItem createDefaultRequest() {
        // Create a default test request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(cn.hutool.core.util.IdUtil.simpleUUID());
        testItem.setName("Default Request");
        testItem.setUrl("https://httpbin.org/get");
        testItem.setMethod("GET");
        // Add some default headers
        testItem.getHeaders().put("User-Agent", "EasyPostman HTTP Client");
        testItem.getHeaders().put("Accept", "*/*");
        return testItem;
    }

    // Default redirect request
    public static HttpRequestItem createDefaultRedirectRequest() {
        // Create a default redirect request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(cn.hutool.core.util.IdUtil.simpleUUID());
        testItem.setName("Redirect Example");
        testItem.setUrl("https://httpbin.org/redirect/1");
        testItem.setMethod("GET");
        // Add some default headers
        testItem.getHeaders().put("User-Agent", "EasyPostman Redirect Client");
        return testItem;
    }

    public static HttpRequestItem createDefaultWebSocketRequest() {
        // Create a default WebSocket request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(cn.hutool.core.util.IdUtil.simpleUUID());
        testItem.setName("WebSocket Example");
        testItem.setUrl("wss://echo.websocket.org");
        testItem.setMethod("GET");
        // Add some default headers
        testItem.getHeaders().put("User-Agent", "EasyPostman WebSocket Client");
        return testItem;
    }
}