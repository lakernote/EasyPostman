package com.laker.postman.plugin.capture;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ProxyRequestTargetTest {

    @Test
    public void shouldNormalizeWebSocketSchemes() {
        ProxyRequestTarget wsTarget = ProxyRequestTarget.resolve(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "ws://example.com/socket",
                Unpooled.EMPTY_BUFFER
        ));
        ProxyRequestTarget wssTarget = ProxyRequestTarget.resolve(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "wss://example.com/socket",
                Unpooled.EMPTY_BUFFER
        ));

        assertEquals(wsTarget.scheme, "http");
        assertEquals(wsTarget.port, 80);
        assertEquals(wssTarget.scheme, "https");
        assertEquals(wssTarget.port, 443);
    }
}
