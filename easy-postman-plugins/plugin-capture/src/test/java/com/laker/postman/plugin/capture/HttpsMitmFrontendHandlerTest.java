package com.laker.postman.plugin.capture;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.testng.annotations.Test;

import javax.net.ssl.SSLHandshakeException;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class HttpsMitmFrontendHandlerTest {

    @Test
    public void shouldCloseWithoutHttpResponseWhenClientRejectsMitmCertificate() {
        CaptureSessionStore sessionStore = new CaptureSessionStore();
        CaptureFilterState captureFilterState = new CaptureFilterState();
        CaptureConnectionContext connectionContext = CaptureConnectionContext.forTest(
                "connection-1",
                CaptureSourceInfo.network("127.0.0.1", 53421, "127.0.0.1", 8888)
                        .withProcess("222", "Cisco Agent", "/Applications/Cisco Agent.app/Contents/MacOS/Cisco Agent")
        );
        HttpsMitmFrontendHandler handler = new HttpsMitmFrontendHandler(
                sessionStore,
                new CaptureCertificateService(),
                captureFilterState,
                "pinned.example.com",
                443,
                connectionContext
        );
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        handler.exceptionCaught(
                channel.pipeline().context(handler),
                new DecoderException(new SSLHandshakeException("Received fatal alert: unknown_ca"))
        );

        assertNull(channel.readOutbound());
        List<CaptureFlow> flows = sessionStore.snapshot();
        assertEquals(flows.size(), 1);
        assertEquals(flows.get(0).method(), "TLS");
        assertEquals(flows.get(0).host(), "pinned.example.com");
        assertEquals(flows.get(0).statusCode(), 495);
        assertEquals(flows.get(0).sourceInfo().tableText(), "Cisco Agent · PID 222");
        assertTrue(flows.get(0).diagnosticsDetailText().contains("CLIENT_TLS"));
        assertTrue(flows.get(0).diagnosticsDetailText().contains("unknown_ca"));
    }

    @Test
    public void shouldSkipDuplicateTlsIssueWhenHandshakeListenerAlreadyReportedFailure() {
        CaptureSessionStore sessionStore = new CaptureSessionStore();
        CaptureFilterState captureFilterState = new CaptureFilterState();
        CaptureConnectionContext connectionContext = CaptureConnectionContext.forTest(
                "connection-1",
                CaptureSourceInfo.network("127.0.0.1", 53421, "127.0.0.1", 8888)
        );
        HttpsMitmFrontendHandler handler = new HttpsMitmFrontendHandler(
                sessionStore,
                new CaptureCertificateService(),
                captureFilterState,
                "pinned.example.com",
                443,
                connectionContext
        );
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(HttpsMitmFrontendHandler.CLIENT_TLS_HANDSHAKE_REPORTED).set(Boolean.TRUE);

        handler.exceptionCaught(
                channel.pipeline().context(handler),
                new DecoderException(new SSLHandshakeException("Received fatal alert: unknown_ca"))
        );

        assertNull(channel.readOutbound());
        assertEquals(sessionStore.snapshot().size(), 0);
    }
}
