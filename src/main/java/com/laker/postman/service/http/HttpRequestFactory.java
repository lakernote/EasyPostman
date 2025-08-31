package com.laker.postman.service.http;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;

public class HttpRequestFactory {
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String EASY_POSTMAN_CLIENT = "EasyPostman Client";

    public static HttpRequestItem createDefaultRequest() {
        // Create a default test request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(cn.hutool.core.util.IdUtil.simpleUUID());
        testItem.setName("Default Request");
        testItem.setUrl("https://httpbin.org/get");
        testItem.setMethod("GET");
        // Add some default headers
        testItem.getHeaders().put(USER_AGENT, EASY_POSTMAN_CLIENT);
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
        testItem.getHeaders().put(USER_AGENT, EASY_POSTMAN_CLIENT);
        return testItem;
    }

    public static HttpRequestItem createDefaultWebSocketRequest() {
        // Create a default WebSocket request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("WebSocket Example");
        testItem.setUrl("wss://echo.websocket.org");
        testItem.setMethod("GET");
        // Add some default headers
        testItem.getHeaders().put(USER_AGENT, EASY_POSTMAN_CLIENT);
        return testItem;
    }

    public static HttpRequestItem createDefaultSseRequest() {
        // Create a default SSE request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setProtocol(RequestItemProtocolEnum.SSE);
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("SSE Example");
        testItem.setUrl("https://sse.dev/test");
        testItem.setMethod("GET");
        testItem.getHeaders().put(USER_AGENT, EASY_POSTMAN_CLIENT);
        testItem.getHeaders().put(ACCEPT, TEXT_EVENT_STREAM);
        return testItem;
    }
}