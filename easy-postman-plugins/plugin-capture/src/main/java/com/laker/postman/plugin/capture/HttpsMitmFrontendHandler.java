package com.laker.postman.plugin.capture;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

@RequiredArgsConstructor
final class HttpsMitmFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger log = LoggerFactory.getLogger(HttpsMitmFrontendHandler.class);
    private static final Duration SOURCE_FILTER_RESOLVE_TIMEOUT = Duration.ofMillis(500);
    static final AttributeKey<Boolean> CLIENT_TLS_HANDSHAKE_REPORTED =
            AttributeKey.valueOf("easyPostman.capture.clientTlsHandshakeReported");

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureFilterState captureFilterState;
    private final String targetHost;
    private final int targetPort;
    private final CaptureConnectionContext connectionContext;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        byte[] requestBody = ByteBufUtil.getBytes(request.content());
        String uri = request.uri() == null || request.uri().isBlank() ? "/" : request.uri();
        String fullUrl = "https://" + targetHost + (targetPort == 443 ? "" : ":" + targetPort) + uri;
        connectionContext.addDiagnostic(CaptureDiagnosticEvent.info(
                CaptureDiagnosticPhase.DECRYPTED_HTTP,
                CaptureDiagnosticRole.HTTPS_MITM_PROXY,
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_HTTPS_DECRYPTED),
                request.method() + " " + uri,
                ""
        ));

        CaptureRequestFilter captureFilter = captureFilterState.current();
        CaptureSourceInfo sourceInfo = sourceInfoForFilter(captureFilter);
        if (!captureFilter.matches(request.method().name(), targetHost, uri, fullUrl, flattenHeaders(request.headers()), sourceInfo)) {
            proxyHttpsWithoutCapture(ctx, request, uri, requestBody, fullUrl);
            return;
        }

        CaptureFlow flow = sessionStore.createFlow(
                request.method().name(),
                fullUrl,
                targetHost,
                uri,
                flattenHeaders(request.headers()),
                requestBody,
                connectionContext.connectionId(),
                connectionContext.sourceInfo(),
                connectionContext.diagnosticSnapshot()
        );

        final SslContext clientSslContext;
        try {
            clientSslContext = certificateService.buildClientSslContext();
        } catch (Exception ex) {
            log.error("Failed to build client SSL context for {}:{}", targetHost, targetPort, ex);
            writeErrorResponse(ctx, flow.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, summarize(ex));
            return;
        }

        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        SslHandler sslHandler = clientSslContext.newHandler(ch.alloc(), targetHost, targetPort);
                        sslHandler.handshakeFuture().addListener(handshakeFuture -> {
                            if (handshakeFuture.isSuccess()) {
                                log.debug("Upstream TLS handshake succeeded for {}:{}", targetHost, targetPort);
                                sessionStore.appendDiagnosticEvent(flow.id(), CaptureDiagnosticEvent.info(
                                        CaptureDiagnosticPhase.TARGET_TLS,
                                        CaptureDiagnosticRole.TARGET_SERVER,
                                        t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_TLS_SUCCEEDED),
                                        targetHost + ":" + targetPort,
                                        ""
                                ));
                            } else {
                                log.warn("Upstream TLS handshake failed for {}:{} - {}", targetHost, targetPort, summarize(handshakeFuture.cause()));
                                sessionStore.appendDiagnosticEvent(flow.id(), CaptureDiagnosticEvent.error(
                                        CaptureDiagnosticPhase.TARGET_TLS,
                                        CaptureDiagnosticRole.TARGET_SERVER,
                                        t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_TLS_FAILED),
                                        targetHost + ":" + targetPort + " - " + summarize(handshakeFuture.cause()),
                                        t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_TLS_FAILED_SUGGESTION)
                                ));
                            }
                        });
                        ch.pipeline().addLast(sslHandler);
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpProxyBackendHandler(ctx.channel(), sessionStore, flow.id()));
                    }
                });

        bootstrap.connect(targetHost, targetPort).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("HTTPS upstream connect failed: {}:{} - {}", targetHost, targetPort, summarize(connectFuture.cause()));
                sessionStore.appendDiagnosticEvent(flow.id(), CaptureDiagnosticEvent.error(
                        CaptureDiagnosticPhase.TARGET_CONNECT,
                        CaptureDiagnosticRole.TARGET_SERVER,
                        t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_CONNECT_FAILED),
                        targetHost + ":" + targetPort + " - " + summarize(connectFuture.cause()),
                        ""
                ));
                writeErrorResponse(ctx, flow.id(), HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "HTTPS upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            sessionStore.appendDiagnosticEvent(flow.id(), CaptureDiagnosticEvent.info(
                    CaptureDiagnosticPhase.TARGET_CONNECT,
                    CaptureDiagnosticRole.TARGET_SERVER,
                    t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_CONNECTED),
                    targetHost + ":" + targetPort,
                    ""
            ));
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpRequest outboundRequest = buildOutboundRequest(request, uri, requestBody);
            upstreamChannel.writeAndFlush(outboundRequest).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    log.warn("HTTPS upstream write failed: {} {} - {}", request.method(), fullUrl, summarize(writeFuture.cause()));
                    sessionStore.appendDiagnosticEvent(flow.id(), CaptureDiagnosticEvent.error(
                            CaptureDiagnosticPhase.TARGET_REQUEST,
                            CaptureDiagnosticRole.TARGET_SERVER,
                            t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_REQUEST_FAILED),
                            request.method() + " " + fullUrl + " - " + summarize(writeFuture.cause()),
                            ""
                    ));
                    sessionStore.fail(flow.id(), HttpResponseStatus.BAD_GATEWAY.code(),
                            writeFuture.cause() == null ? "Failed to send HTTPS upstream request" : summarize(writeFuture.cause()));
                    writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY, "Failed to send HTTPS upstream request");
                    upstreamChannel.close();
                } else {
                    sessionStore.appendDiagnosticEvent(flow.id(), CaptureDiagnosticEvent.info(
                            CaptureDiagnosticPhase.TARGET_REQUEST,
                            CaptureDiagnosticRole.EASY_POSTMAN_PROXY,
                            t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_TARGET_REQUEST_SENT),
                            request.method() + " " + fullUrl,
                            ""
                    ));
                }
            });
        });
    }

    private void proxyHttpsWithoutCapture(ChannelHandlerContext ctx,
                                          FullHttpRequest request,
                                          String uri,
                                          byte[] requestBody,
                                          String fullUrl) {
        final SslContext clientSslContext;
        try {
            clientSslContext = certificateService.buildClientSslContext();
        } catch (Exception ex) {
            log.error("Failed to build client SSL context for passthrough {}:{}", targetHost, targetPort, ex);
            writeSimpleResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, summarize(ex));
            return;
        }

        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(clientSslContext.newHandler(ch.alloc(), targetHost, targetPort));
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpProxyBackendHandler(ctx.channel()));
                    }
                });

        bootstrap.connect(targetHost, targetPort).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("HTTPS passthrough connect failed: {} - {}", fullUrl, summarize(connectFuture.cause()));
                writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "HTTPS upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpRequest outboundRequest = buildOutboundRequest(request, uri, requestBody);
            upstreamChannel.writeAndFlush(outboundRequest).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    log.warn("HTTPS passthrough write failed: {} - {}", fullUrl, summarize(writeFuture.cause()));
                    writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY, "Failed to send HTTPS upstream request");
                    upstreamChannel.close();
                }
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (isClientTlsHandshakeFailure(cause)) {
            if (markClientTlsHandshakeReported(ctx)) {
                log.debug("HTTPS MITM client TLS handshake failure already reported for {}:{} - {}", targetHost, targetPort, summarize(cause));
            } else if (recordClientTlsHandshakeFailure(cause)) {
                log.warn("HTTPS MITM client TLS handshake failed for {}:{} - {}", targetHost, targetPort, summarize(cause));
            } else {
                log.debug("Repeated HTTPS MITM client TLS handshake failed for {}:{} - {}", targetHost, targetPort, summarize(cause));
            }
            ctx.close();
            return;
        }
        log.error("HTTPS MITM frontend request failed for {}:{}", targetHost, targetPort, cause);
        writeSimpleResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                cause == null ? "HTTPS MITM request failed" : summarize(cause));
    }

    private boolean isClientTlsHandshakeFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SSLHandshakeException) {
                return true;
            }
            Throwable next = current.getCause();
            if (next == null || next == current) {
                return false;
            }
            current = next;
        }
        return false;
    }

    private boolean recordClientTlsHandshakeFailure(Throwable cause) {
        connectionContext.addDiagnostic(CaptureDiagnosticEvent.error(
                CaptureDiagnosticPhase.CLIENT_TLS,
                CaptureDiagnosticRole.SOURCE_APP,
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_CLIENT_TLS_REJECTED),
                summarize(cause),
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_CLIENT_TLS_REJECTED_SUGGESTION)
        ));
        return sessionStore.recordTlsIssue(
                targetHost,
                targetPort,
                t(MessageKeys.TOOLBOX_CAPTURE_TLS_CLIENT_REJECTED, targetHost, summarize(cause)),
                connectionContext.connectionId(),
                connectionContext.sourceInfo(),
                connectionContext.diagnosticSnapshot()
        );
    }

    private boolean markClientTlsHandshakeReported(ChannelHandlerContext ctx) {
        return Boolean.TRUE.equals(ctx.channel().attr(CLIENT_TLS_HANDSHAKE_REPORTED).getAndSet(Boolean.TRUE));
    }

    private FullHttpRequest buildOutboundRequest(FullHttpRequest request, String uri, byte[] requestBody) {
        boolean webSocketUpgrade = isWebSocketUpgradeRequest(request);
        FullHttpRequest outbound = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                request.method(),
                uri,
                Unpooled.wrappedBuffer(requestBody)
        );
        request.headers().forEach(entry -> {
            String name = entry.getKey();
            if ((!webSocketUpgrade && HttpHeaderNames.CONNECTION.contentEqualsIgnoreCase(name))
                    || HttpHeaderNames.HOST.contentEqualsIgnoreCase(name)) {
                return;
            }
            outbound.headers().add(name, entry.getValue());
        });
        outbound.headers().set(HttpHeaderNames.HOST, targetPort == 443 ? targetHost : targetHost + ":" + targetPort);
        if (!webSocketUpgrade) {
            outbound.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        if (requestBody.length > 0 || !webSocketUpgrade) {
            HttpUtil.setContentLength(outbound, requestBody.length);
        }
        return outbound;
    }

    private CaptureSourceInfo sourceInfoForFilter(CaptureRequestFilter captureFilter) {
        if (captureFilter != null && captureFilter.requiresResolvedSource()) {
            return connectionContext.awaitSourceInfo(SOURCE_FILTER_RESOLVE_TIMEOUT);
        }
        return connectionContext.sourceInfo();
    }

    private boolean isWebSocketUpgradeRequest(FullHttpRequest request) {
        return "websocket".equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE));
    }

    private Map<String, String> flattenHeaders(io.netty.handler.codec.http.HttpHeaders headers) {
        Map<String, String> flattened = new LinkedHashMap<>();
        headers.forEach(entry -> flattened.put(entry.getKey(), entry.getValue()));
        return flattened;
    }

    private void writeErrorResponse(ChannelHandlerContext ctx, String flowId, HttpResponseStatus status, String message) {
        sessionStore.fail(flowId, status.code(), message);
        writeSimpleResponse(ctx, status, message);
    }

    private void writeSimpleResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        byte[] body = (message == null ? "" : message).getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(body));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        HttpUtil.setContentLength(response, body.length);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
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
}
