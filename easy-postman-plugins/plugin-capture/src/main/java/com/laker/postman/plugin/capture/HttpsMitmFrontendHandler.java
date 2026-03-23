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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class HttpsMitmFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final int MAX_HTTP_OBJECT_SIZE = 10 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(HttpsMitmFrontendHandler.class);

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureRequestFilter captureRequestFilter;
    private final String targetHost;
    private final int targetPort;

    HttpsMitmFrontendHandler(CaptureSessionStore sessionStore,
                             CaptureCertificateService certificateService,
                             CaptureRequestFilter captureRequestFilter,
                             String targetHost,
                             int targetPort) {
        this.sessionStore = sessionStore;
        this.certificateService = certificateService;
        this.captureRequestFilter = captureRequestFilter;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        byte[] requestBody = ByteBufUtil.getBytes(request.content());
        String uri = request.uri() == null || request.uri().isBlank() ? "/" : request.uri();
        String fullUrl = "https://" + targetHost + (targetPort == 443 ? "" : ":" + targetPort) + uri;

        if (!captureRequestFilter.matches(targetHost, uri, fullUrl)) {
            proxyHttpsWithoutCapture(ctx, request, uri, requestBody, fullUrl);
            return;
        }

        CaptureFlow flow = sessionStore.createFlow(
                request.method().name(),
                fullUrl,
                targetHost,
                uri,
                flattenHeaders(request.headers()),
                requestBody
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
                                log.info("Upstream TLS handshake succeeded for {}:{}", targetHost, targetPort);
                            } else {
                                log.warn("Upstream TLS handshake failed for {}:{} - {}", targetHost, targetPort, summarize(handshakeFuture.cause()));
                            }
                        });
                        ch.pipeline().addLast(sslHandler);
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
                        ch.pipeline().addLast(new HttpProxyBackendHandler(ctx.channel(), sessionStore, flow.id()));
                    }
                });

        bootstrap.connect(targetHost, targetPort).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("HTTPS upstream connect failed: {}:{} - {}", targetHost, targetPort, summarize(connectFuture.cause()));
                writeErrorResponse(ctx, flow.id(), HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "HTTPS upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpRequest outboundRequest = buildOutboundRequest(request, uri, requestBody);
            upstreamChannel.writeAndFlush(outboundRequest).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    log.warn("HTTPS upstream write failed: {} {} - {}", request.method(), fullUrl, summarize(writeFuture.cause()));
                    sessionStore.fail(flow.id(), HttpResponseStatus.BAD_GATEWAY.code(),
                            writeFuture.cause() == null ? "Failed to send HTTPS upstream request" : summarize(writeFuture.cause()));
                    writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY, "Failed to send HTTPS upstream request");
                    upstreamChannel.close();
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
                        ch.pipeline().addLast(new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
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
        log.error("HTTPS MITM frontend request failed for {}:{}", targetHost, targetPort, cause);
        writeSimpleResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                cause == null ? "HTTPS MITM request failed" : summarize(cause));
    }

    private FullHttpRequest buildOutboundRequest(FullHttpRequest request, String uri, byte[] requestBody) {
        FullHttpRequest outbound = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                request.method(),
                uri,
                Unpooled.wrappedBuffer(requestBody)
        );
        request.headers().forEach(entry -> {
            String name = entry.getKey();
            if (HttpHeaderNames.CONNECTION.contentEqualsIgnoreCase(name)
                    || HttpHeaderNames.HOST.contentEqualsIgnoreCase(name)) {
                return;
            }
            outbound.headers().add(name, entry.getValue());
        });
        outbound.headers().set(HttpHeaderNames.HOST, targetPort == 443 ? targetHost : targetHost + ":" + targetPort);
        outbound.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        HttpUtil.setContentLength(outbound, requestBody.length);
        return outbound;
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
