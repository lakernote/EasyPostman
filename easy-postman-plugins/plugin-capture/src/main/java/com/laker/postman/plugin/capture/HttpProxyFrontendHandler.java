package com.laker.postman.plugin.capture;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class HttpProxyFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final int MAX_HTTP_OBJECT_SIZE = 10 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(HttpProxyFrontendHandler.class);

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureRequestFilter captureRequestFilter;

    HttpProxyFrontendHandler(CaptureSessionStore sessionStore,
                             CaptureCertificateService certificateService,
                             CaptureRequestFilter captureRequestFilter) {
        this.sessionStore = sessionStore;
        this.certificateService = certificateService;
        this.captureRequestFilter = captureRequestFilter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (HttpMethod.CONNECT.equals(request.method())) {
            handleConnect(ctx, request);
            return;
        }

        ProxyRequestTarget target;
        try {
            target = ProxyRequestTarget.resolve(request);
        } catch (Exception ex) {
            writeErrorResponse(ctx, null, HttpResponseStatus.BAD_REQUEST, ex.getMessage());
            return;
        }
        if (!"http".equalsIgnoreCase(target.scheme)) {
            writeErrorResponse(ctx, null, HttpResponseStatus.NOT_IMPLEMENTED, "Only HTTP explicit proxy is supported in this MVP");
            return;
        }

        if (!captureRequestFilter.matches(target.host, target.requestUri, target.fullUrl, flattenHeaders(request.headers()))) {
            proxyHttpWithoutCapture(ctx, request, target);
            return;
        }

        byte[] requestBody = ByteBufUtil.getBytes(request.content());
        CaptureFlow flow = sessionStore.createFlow(
                request.method().name(),
                target.fullUrl,
                target.host,
                target.requestUri,
                flattenHeaders(request.headers()),
                requestBody
        );

        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpProxyBackendHandler(ctx.channel(), sessionStore, flow.id()));
                    }
                });

        bootstrap.connect(target.host, target.port).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("HTTP upstream connect failed: {}:{} - {}", target.host, target.port, summarize(connectFuture.cause()));
                writeErrorResponse(ctx, flow.id(), HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "Upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpRequest outboundRequest = buildOutboundRequest(request, target, requestBody);
            upstreamChannel.writeAndFlush(outboundRequest).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    log.warn("HTTP upstream write failed: {} {} - {}", request.method(), target.fullUrl, summarize(writeFuture.cause()));
                    sessionStore.fail(flow.id(), HttpResponseStatus.BAD_GATEWAY.code(),
                            writeFuture.cause() == null ? "Failed to send upstream request" : summarize(writeFuture.cause()));
                    writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY, "Failed to send upstream request");
                    upstreamChannel.close();
                }
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Frontend proxy request failed", cause);
        writeSimpleResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                cause == null ? "Proxy request failed" : summarize(cause));
    }

    private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest request) {
        String authority = request.uri();
        String host = authority;
        int port = 443;
        int colonIndex = authority.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < authority.length() - 1) {
            host = authority.substring(0, colonIndex);
            port = Integer.parseInt(authority.substring(colonIndex + 1));
        }
        log.debug("CONNECT request received: {} -> {}:{}", authority, host, port);

        if (!captureRequestFilter.shouldMitmHost(host)) {
            establishDirectTunnel(ctx, authority, host, port);
            return;
        }

        final SslContext serverSslContext;
        try {
            serverSslContext = certificateService.buildServerSslContext(host);
            log.debug("MITM server certificate prepared for {}", host);
        } catch (Exception ex) {
            log.error("Failed to initialize MITM certificate for {}", host, ex);
            CaptureFlow flow = sessionStore.createFlow(
                    request.method().name(),
                    request.uri(),
                    request.uri(),
                    request.uri(),
                    flattenHeaders(request.headers()),
                    ByteBufUtil.getBytes(request.content())
            );
            sessionStore.fail(flow.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                    "Failed to initialize MITM certificate: " + summarize(ex));
            writeSimpleResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "Failed to initialize MITM certificate: " + summarize(ex));
            return;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                new HttpResponseStatus(200, "Connection Established")
        );
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        HttpUtil.setContentLength(response, 0);
        int targetPort = port;
        String targetHost = host;
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.warn("Failed to acknowledge CONNECT tunnel for {}: {}", authority, summarize(future.cause()));
                ctx.close();
                return;
            }
            log.debug("CONNECT tunnel acknowledged for {}", authority);
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.remove(HttpServerCodec.class);
            pipeline.remove(HttpObjectAggregator.class);
            pipeline.remove(this);
            SslHandler sslHandler = serverSslContext.newHandler(ctx.alloc());
            sslHandler.handshakeFuture().addListener(handshakeFuture -> {
                if (handshakeFuture.isSuccess()) {
                    log.debug("Client TLS handshake succeeded for {}", authority);
                } else {
                    log.warn("Client TLS handshake failed for {}: {}", authority, summarize(handshakeFuture.cause()));
                }
            });
            pipeline.addLast("mitm-ssl", sslHandler);
            pipeline.addLast("httpsServerCodec", new HttpServerCodec());
            pipeline.addLast("httpsAggregator", new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
            pipeline.addLast("httpsFrontendHandler", new HttpsMitmFrontendHandler(
                    sessionStore,
                    certificateService,
                    captureRequestFilter,
                    targetHost,
                    targetPort
            ));
        });
    }

    private void proxyHttpWithoutCapture(ChannelHandlerContext ctx, FullHttpRequest request, ProxyRequestTarget target) {
        byte[] requestBody = ByteBufUtil.getBytes(request.content());
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpProxyBackendHandler(ctx.channel()));
                    }
                });

        bootstrap.connect(target.host, target.port).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("HTTP upstream connect failed: {}:{} - {}", target.host, target.port, summarize(connectFuture.cause()));
                writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "Upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpRequest outboundRequest = buildOutboundRequest(request, target, requestBody);
            upstreamChannel.writeAndFlush(outboundRequest).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    log.warn("HTTP upstream write failed: {} {} - {}", request.method(), target.fullUrl, summarize(writeFuture.cause()));
                    writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY, "Failed to send upstream request");
                    upstreamChannel.close();
                }
            });
        });
    }

    private void establishDirectTunnel(ChannelHandlerContext ctx, String authority, String host, int port) {
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new DirectRelayHandler(ctx.channel()));
                    }
                });

        bootstrap.connect(host, port).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("Direct tunnel connect failed: {}:{} - {}", host, port, summarize(connectFuture.cause()));
                writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "Upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    new HttpResponseStatus(200, "Connection Established")
            );
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            HttpUtil.setContentLength(response, 0);
            ctx.writeAndFlush(response).addListener(future -> {
                if (!future.isSuccess()) {
                    log.warn("Failed to acknowledge direct tunnel for {}: {}", authority, summarize(future.cause()));
                    upstreamChannel.close();
                    ctx.close();
                    return;
                }
                log.debug("Direct tunnel acknowledged for {}", authority);
                ChannelPipeline pipeline = ctx.pipeline();
                pipeline.remove(HttpServerCodec.class);
                pipeline.remove(HttpObjectAggregator.class);
                pipeline.remove(this);
                pipeline.addLast("directRelay", new DirectRelayHandler(upstreamChannel));
                ctx.channel().read();
            });
        });
    }

    private FullHttpRequest buildOutboundRequest(FullHttpRequest request, ProxyRequestTarget target, byte[] requestBody) {
        boolean webSocketUpgrade = isWebSocketUpgradeRequest(request);
        FullHttpRequest outbound = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                request.method(),
                target.requestUri,
                Unpooled.wrappedBuffer(requestBody)
        );
        request.headers().forEach(entry -> {
            String name = entry.getKey();
            if (HttpHeaderNames.PROXY_CONNECTION.contentEqualsIgnoreCase(name)
                    || (!webSocketUpgrade && HttpHeaderNames.CONNECTION.contentEqualsIgnoreCase(name))
                    || HttpHeaderNames.HOST.contentEqualsIgnoreCase(name)) {
                return;
            }
            outbound.headers().add(name, entry.getValue());
        });
        outbound.headers().set(HttpHeaderNames.HOST, target.port == 80 ? target.host : target.host + ":" + target.port);
        if (!webSocketUpgrade) {
            outbound.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        HttpUtil.setContentLength(outbound, requestBody.length);
        return outbound;
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
        if (flowId != null) {
            sessionStore.fail(flowId, status.code(), message);
        }
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
