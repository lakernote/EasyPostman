package com.laker.postman.plugin.capture;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaderNames;
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

    @Test
    public void shouldParseBracketedIpv6HostHeaders() {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                "/health",
                Unpooled.EMPTY_BUFFER
        );
        request.headers().set(HttpHeaderNames.HOST, "[::1]:8080");

        ProxyRequestTarget target = ProxyRequestTarget.resolve(request);

        assertEquals(target.host, "::1");
        assertEquals(target.port, 8080);
        assertEquals(target.fullUrl, "http://[::1]:8080/health");
    }

    @Test
    public void shouldParseIpv6AuthorityForConnect() {
        ProxyRequestTarget.HostPort hostPort = ProxyRequestTarget.parseAuthority("[2001:db8::1]:9443", 443);

        assertEquals(hostPort.host(), "2001:db8::1");
        assertEquals(hostPort.port(), 9443);
        assertEquals(hostPort.authorityForUri(443), "[2001:db8::1]:9443");
    }

    @Test
    public void shouldTreatBareIpv6AuthorityAsHostWithoutPort() {
        ProxyRequestTarget.HostPort hostPort = ProxyRequestTarget.parseAuthority("2001:db8::1", 443);

        assertEquals(hostPort.host(), "2001:db8::1");
        assertEquals(hostPort.port(), 443);
        assertEquals(hostPort.authorityForUri(443), "[2001:db8::1]");
    }
}
