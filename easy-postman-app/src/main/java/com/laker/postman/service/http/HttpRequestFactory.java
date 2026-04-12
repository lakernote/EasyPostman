package com.laker.postman.service.http;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.util.SystemUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.service.collections.DefaultRequestsFactory.APPLICATION_JSON;
import static com.laker.postman.service.collections.DefaultRequestsFactory.CONTENT_TYPE;

@UtilityClass
public class HttpRequestFactory {
    private static final String DEFAULT_USER_AGENT_VERSION = "dev";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String EASY_POSTMAN_CLIENT = "EasyPostman/" + resolveUserAgentVersion();
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String CONNECTION = "Connection";
    public static final String ACCEPT_ENCODING_VALUE = "gzip, deflate, br";
    public static final String CONNECTION_VALUE = "keep-alive";

    private static String resolveUserAgentVersion() {
        String version = SystemUtil.getCurrentVersion();
        if (version == null || version.isBlank()) {
            return DEFAULT_USER_AGENT_VERSION;
        }
        return StandardCharsets.US_ASCII.newEncoder().canEncode(version)
                ? version
                : DEFAULT_USER_AGENT_VERSION;
    }

    public static HttpRequestItem createDefaultRequest() {
        // Create a default test request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("Default Request");
        testItem.setUrl("https://httpbin.org/get");
        testItem.setMethod("GET");
        // Add some default headers (using ArrayList to ensure mutability)
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, USER_AGENT, EASY_POSTMAN_CLIENT));
        headers.add(new HttpHeader(true, ACCEPT, "*/*"));
        headers.add(new HttpHeader(true, ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE));
        headers.add(new HttpHeader(true, CONNECTION, CONNECTION_VALUE));
        testItem.setHeadersList(headers);
        return testItem;
    }

    // Default redirect request
    public static HttpRequestItem createDefaultRedirectRequest() {
        // Create a default redirect request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("Redirect Example");
        testItem.setUrl("https://httpbin.org/redirect/1");
        testItem.setMethod("GET");
        // Add some default headers
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, USER_AGENT, EASY_POSTMAN_CLIENT));
        headers.add(new HttpHeader(true, ACCEPT, "*/*"));
        headers.add(new HttpHeader(true, ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE));
        headers.add(new HttpHeader(true, CONNECTION, CONNECTION_VALUE));
        testItem.setHeadersList(headers);
        return testItem;
    }

    public static HttpRequestItem createDefaultWebSocketRequest() {
        // Create a default WebSocket request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("WebSocket Example");
        testItem.setUrl("wss://ws.ifelse.io");
        testItem.setMethod("GET");
        // Add some default headers
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, USER_AGENT, EASY_POSTMAN_CLIENT));
        headers.add(new HttpHeader(true, ACCEPT, "*/*"));
        headers.add(new HttpHeader(true, ACCEPT_ENCODING, "identity"));
        headers.add(new HttpHeader(true, CONNECTION, CONNECTION_VALUE));
        headers.add(new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON));
        testItem.setHeadersList(headers);
        testItem.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        return testItem;
    }

    public static HttpRequestItem createDefaultSseRequest() {
        // Create a default SSE request
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setProtocol(RequestItemProtocolEnum.SSE);
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("SSE Example");
        testItem.setUrl("https://stream.wikimedia.org/v2/stream/recentchange");
        testItem.setMethod("GET");
        // Add some default headers
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(true, USER_AGENT, EASY_POSTMAN_CLIENT));
        headers.add(new HttpHeader(true, ACCEPT, TEXT_EVENT_STREAM));
        headers.add(new HttpHeader(true, ACCEPT_ENCODING, "identity"));
        headers.add(new HttpHeader(true, CONNECTION, CONNECTION_VALUE));
        testItem.setHeadersList(headers);
        return testItem;
    }
}
