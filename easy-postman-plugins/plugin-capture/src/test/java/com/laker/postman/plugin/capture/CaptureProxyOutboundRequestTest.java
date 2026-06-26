package com.laker.postman.plugin.capture;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureProxyOutboundRequestTest {

    @Test
    public void shouldNotAddContentLengthZeroToHttpWebSocketUpgradeRequest() throws Exception {
        FullHttpRequest request = websocketRequest("http://echo.example.com/socket");
        ProxyRequestTarget target = ProxyRequestTarget.resolve(request);
        HttpProxyFrontendHandler handler = new HttpProxyFrontendHandler(
                new CaptureSessionStore(),
                new CaptureCertificateService(),
                new CaptureFilterState(),
                CaptureConnectionContext.forTest("connection-1", CaptureSourceInfo.unknown()));

        FullHttpRequest outbound = invokeHttpOutboundRequest(handler, request, target, new byte[0]);

        assertFalse(outbound.headers().contains(HttpHeaderNames.CONTENT_LENGTH));
        assertTrue(HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(outbound.headers().get(HttpHeaderNames.CONNECTION)));
    }

    @Test
    public void shouldNotAddContentLengthZeroToHttpsWebSocketUpgradeRequest() throws Exception {
        FullHttpRequest request = websocketRequest("/socket");
        HttpsMitmFrontendHandler handler = new HttpsMitmFrontendHandler(
                new CaptureSessionStore(),
                new CaptureCertificateService(),
                new CaptureFilterState(),
                "echo.example.com",
                443,
                CaptureConnectionContext.forTest("connection-1", CaptureSourceInfo.unknown()));

        FullHttpRequest outbound = invokeHttpsOutboundRequest(handler, request, "/socket", new byte[0]);

        assertFalse(outbound.headers().contains(HttpHeaderNames.CONTENT_LENGTH));
        assertTrue(HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(outbound.headers().get(HttpHeaderNames.CONNECTION)));
    }

    private static FullHttpRequest websocketRequest(String uri) {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                uri,
                Unpooled.EMPTY_BUFFER);
        request.headers().set(HttpHeaderNames.HOST, "echo.example.com");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
        request.headers().set(HttpHeaderNames.UPGRADE, "websocket");
        request.headers().set("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");
        request.headers().set("Sec-WebSocket-Version", "13");
        return request;
    }

    private static FullHttpRequest invokeHttpOutboundRequest(HttpProxyFrontendHandler handler,
                                                             FullHttpRequest request,
                                                             ProxyRequestTarget target,
                                                             byte[] requestBody) throws Exception {
        Method method = HttpProxyFrontendHandler.class.getDeclaredMethod(
                "buildOutboundRequest",
                FullHttpRequest.class,
                ProxyRequestTarget.class,
                byte[].class);
        method.setAccessible(true);
        return (FullHttpRequest) method.invoke(handler, request, target, requestBody);
    }

    private static FullHttpRequest invokeHttpsOutboundRequest(HttpsMitmFrontendHandler handler,
                                                              FullHttpRequest request,
                                                              String uri,
                                                              byte[] requestBody) throws Exception {
        Method method = HttpsMitmFrontendHandler.class.getDeclaredMethod(
                "buildOutboundRequest",
                FullHttpRequest.class,
                String.class,
                byte[].class);
        method.setAccessible(true);
        return (FullHttpRequest) method.invoke(handler, request, uri, requestBody);
    }
}
