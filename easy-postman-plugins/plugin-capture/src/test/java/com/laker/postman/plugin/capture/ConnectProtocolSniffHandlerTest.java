package com.laker.postman.plugin.capture;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class ConnectProtocolSniffHandlerTest {

    @Test
    public void shouldDetectPlainHttpWebSocketUpgradeInsideConnectTunnel() {
        byte[] request = """
                GET / HTTP/1.1\r
                Host: 124.222.6.60:8800\r
                Connection: Upgrade\r
                Upgrade: websocket\r
                Sec-WebSocket-Version: 13\r
                \r
                """.getBytes(StandardCharsets.US_ASCII);

        assertEquals(
                ConnectProtocolSniffHandler.detectProtocol(Unpooled.wrappedBuffer(request)),
                ConnectProtocolSniffHandler.Protocol.HTTP
        );
    }

    @Test
    public void shouldDetectTlsRecordInsideConnectTunnel() {
        byte[] tlsClientHelloPrefix = new byte[]{0x16, 0x03, 0x03, 0x00, 0x2f};

        assertEquals(
                ConnectProtocolSniffHandler.detectProtocol(Unpooled.wrappedBuffer(tlsClientHelloPrefix)),
                ConnectProtocolSniffHandler.Protocol.TLS
        );
    }

    @Test
    public void shouldRoutePlainHttpConnectTunnelWithoutInstallingTlsHandler() {
        EmbeddedChannel channel = new EmbeddedChannel(new ConnectProtocolSniffHandler(
                new CaptureSessionStore(),
                new CaptureCertificateService(),
                new CaptureFilterState(),
                "124.222.6.60",
                8800,
                "124.222.6.60:8800",
                CaptureConnectionContext.forTest("connection-1", CaptureSourceInfo.unknown())
        ));

        channel.writeInbound(Unpooled.copiedBuffer("GET ", StandardCharsets.US_ASCII));

        assertNotNull(channel.pipeline().get("connectPlainFrontendHandler"));
        assertNull(channel.pipeline().get(SslHandler.class));
    }
}
