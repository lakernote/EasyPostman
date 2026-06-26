package com.laker.postman.plugin.capture;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.nio.channels.ClosedChannelException;
import java.util.List;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

@RequiredArgsConstructor
final class ConnectProtocolSniffHandler extends ByteToMessageDecoder {
    private static final int MAX_HTTP_OBJECT_SIZE = 10 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(ConnectProtocolSniffHandler.class);

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureFilterState captureFilterState;
    private final String targetHost;
    private final int targetPort;
    private final String authority;
    private final CaptureConnectionContext connectionContext;
    private boolean routed;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (routed || in.readableBytes() < 3) {
            return;
        }
        Protocol protocol = detectProtocol(in);
        if (protocol == Protocol.UNKNOWN && in.readableBytes() < 8) {
            return;
        }
        routed = true;
        if (protocol == Protocol.HTTP) {
            installPlainHttpHandlers(ctx);
        } else {
            installTlsHandlers(ctx);
        }
        out.add(in.readRetainedSlice(in.readableBytes()));
        ctx.pipeline().remove(this);
    }

    static Protocol detectProtocol(ByteBuf bytes) {
        if (bytes == null || bytes.readableBytes() < 3) {
            return Protocol.UNKNOWN;
        }
        int index = bytes.readerIndex();
        int first = bytes.getUnsignedByte(index);
        int second = bytes.getUnsignedByte(index + 1);
        if (isTlsRecordType(first) && second == 0x03) {
            return Protocol.TLS;
        }
        if (looksLikeHttpMethod(bytes, index)) {
            return Protocol.HTTP;
        }
        return Protocol.UNKNOWN;
    }

    private static boolean isTlsRecordType(int value) {
        return value >= 0x14 && value <= 0x17;
    }

    private static boolean looksLikeHttpMethod(ByteBuf bytes, int index) {
        return startsWith(bytes, index, "GET ")
                || startsWith(bytes, index, "POST ")
                || startsWith(bytes, index, "HEAD ")
                || startsWith(bytes, index, "PUT ")
                || startsWith(bytes, index, "PATCH ")
                || startsWith(bytes, index, "DELETE ")
                || startsWith(bytes, index, "OPTIONS ")
                || startsWith(bytes, index, "TRACE ");
    }

    private static boolean startsWith(ByteBuf bytes, int index, String prefix) {
        if (bytes.readableBytes() < prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (bytes.getByte(index + i) != (byte) prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void installPlainHttpHandlers(ChannelHandlerContext ctx) {
        connectionContext.addDiagnostic(CaptureDiagnosticEvent.info(
                CaptureDiagnosticPhase.HTTP_REQUEST,
                CaptureDiagnosticRole.CLIENT_CONNECTION,
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_CONNECT_PLAIN_HTTP),
                authority,
                ""
        ));
        ChannelPipeline pipeline = ctx.pipeline();
        String name = ctx.name();
        pipeline.addAfter(name, "connectPlainServerCodec", new HttpServerCodec());
        pipeline.addAfter("connectPlainServerCodec", "connectPlainAggregator", new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
        pipeline.addAfter("connectPlainAggregator", "connectPlainFrontendHandler", new HttpConnectPlainFrontendHandler(
                sessionStore,
                captureFilterState,
                targetHost,
                targetPort,
                connectionContext
        ));
    }

    private void installTlsHandlers(ChannelHandlerContext ctx) {
        SslContext serverSslContext;
        try {
            serverSslContext = certificateService.buildServerSslContext(targetHost);
            log.debug("MITM server certificate prepared for {}", targetHost);
            connectionContext.addDiagnostic(CaptureDiagnosticEvent.info(
                    CaptureDiagnosticPhase.MITM_CERT,
                    CaptureDiagnosticRole.HTTPS_MITM_PROXY,
                    t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_MITM_CERT_PREPARED),
                    targetHost,
                    ""
            ));
        } catch (Exception ex) {
            log.error("Failed to initialize MITM certificate for {}", targetHost, ex);
            sessionStore.recordTlsIssue(
                    targetHost,
                    targetPort,
                    "Failed to initialize MITM certificate: " + summarize(ex),
                    connectionContext.connectionId(),
                    connectionContext.sourceInfo(),
                    connectionContext.diagnosticSnapshot()
            );
            ctx.close();
            return;
        }

        SslHandler sslHandler = serverSslContext.newHandler(ctx.alloc());
        sslHandler.handshakeFuture().addListener(handshakeFuture -> {
            if (handshakeFuture.isSuccess()) {
                log.debug("Client TLS handshake succeeded for {}", authority);
                connectionContext.addDiagnostic(CaptureDiagnosticEvent.info(
                        CaptureDiagnosticPhase.CLIENT_TLS,
                        CaptureDiagnosticRole.SOURCE_APP,
                        t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_CLIENT_TLS_ACCEPTED),
                        authority,
                        ""
                ));
            } else {
                recordClientTlsHandshakeFailure(ctx, handshakeFuture.cause());
            }
        });
        ChannelPipeline pipeline = ctx.pipeline();
        String name = ctx.name();
        pipeline.addAfter(name, "mitm-ssl", sslHandler);
        pipeline.addAfter("mitm-ssl", "httpsServerCodec", new HttpServerCodec());
        pipeline.addAfter("httpsServerCodec", "httpsAggregator", new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
        pipeline.addAfter("httpsAggregator", "httpsFrontendHandler", new HttpsMitmFrontendHandler(
                sessionStore,
                certificateService,
                captureFilterState,
                targetHost,
                targetPort,
                connectionContext
        ));
    }

    private void recordClientTlsHandshakeFailure(ChannelHandlerContext ctx, Throwable cause) {
        if (markClientTlsHandshakeReported(ctx)) {
            log.debug("Client TLS handshake failure already reported for {}:{} - {}", targetHost, targetPort, summarize(cause));
            return;
        }
        connectionContext.addDiagnostic(CaptureDiagnosticEvent.error(
                CaptureDiagnosticPhase.CLIENT_TLS,
                CaptureDiagnosticRole.SOURCE_APP,
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_CLIENT_TLS_REJECTED),
                summarize(cause),
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_CLIENT_TLS_REJECTED_SUGGESTION)
        ));
        boolean recorded = sessionStore.recordTlsIssue(
                targetHost,
                targetPort,
                t(MessageKeys.TOOLBOX_CAPTURE_TLS_CLIENT_HANDSHAKE_FAILED, targetHost, summarize(cause)),
                connectionContext.connectionId(),
                connectionContext.sourceInfo(),
                connectionContext.diagnosticSnapshot()
        );
        if (isClientHandshakeAbort(cause)) {
            log.debug("Client closed MITM TLS handshake for {}:{} - {}", targetHost, targetPort, summarize(cause));
            return;
        }
        if (recorded) {
            log.warn("Client TLS handshake failed for {}:{} - {}", targetHost, targetPort, summarize(cause));
        } else {
            log.debug("Repeated client TLS handshake failed for {}:{} - {}", targetHost, targetPort, summarize(cause));
        }
    }

    private boolean markClientTlsHandshakeReported(ChannelHandlerContext ctx) {
        return Boolean.TRUE.equals(ctx.channel().attr(HttpsMitmFrontendHandler.CLIENT_TLS_HANDSHAKE_REPORTED).getAndSet(Boolean.TRUE));
    }

    private boolean isClientHandshakeAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ClosedChannelException) {
                return true;
            }
            if (current instanceof SSLHandshakeException) {
                return false;
            }
            Throwable next = current.getCause();
            if (next == null || next == current) {
                return false;
            }
            current = next;
        }
        return false;
    }

    private String summarize(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        return root.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    enum Protocol {
        TLS,
        HTTP,
        UNKNOWN
    }
}
