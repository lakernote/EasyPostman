package com.laker.postman.plugin.capture;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.testng.annotations.Test;

import javax.net.ssl.SSLHandshakeException;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class HttpsMitmFrontendHandlerTest {

    @Test
    public void shouldCloseWithoutHttpResponseWhenClientRejectsMitmCertificate() {
        CaptureSessionStore sessionStore = new CaptureSessionStore();
        CaptureFilterState captureFilterState = new CaptureFilterState();
        HttpsMitmFrontendHandler handler = new HttpsMitmFrontendHandler(
                sessionStore,
                new CaptureCertificateService(),
                captureFilterState,
                "pinned.example.com",
                443
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
    }
}
