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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

@RequiredArgsConstructor
final class HttpConnectPlainFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Duration SOURCE_FILTER_RESOLVE_TIMEOUT = Duration.ofMillis(500);
    private static final Logger log = LoggerFactory.getLogger(HttpConnectPlainFrontendHandler.class);

    private final CaptureSessionStore sessionStore;
    private final CaptureFilterState captureFilterState;
    private final String targetHost;
    private final int targetPort;
    private final CaptureConnectionContext connectionContext;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        byte[] requestBody = ByteBufUtil.getBytes(request.content());
        String uri = request.uri() == null || request.uri().isBlank() ? "/" : request.uri();
        String fullUrl = "http://" + targetHost + (targetPort == 80 ? "" : ":" + targetPort) + uri;
        connectionContext.addDiagnostic(CaptureDiagnosticEvent.info(
                CaptureDiagnosticPhase.HTTP_REQUEST,
                CaptureDiagnosticRole.CLIENT_CONNECTION,
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_HTTP_REQUEST_RECEIVED),
                request.method() + " " + uri,
                ""
        ));

        CaptureRequestFilter captureFilter = captureFilterState.current();
        CaptureSourceInfo sourceInfo = sourceInfoForFilter(captureFilter);
        if (!captureFilter.matches(request.method().name(), targetHost, uri, fullUrl, flattenHeaders(request.headers()), sourceInfo)) {
            proxy(ctx, request, uri, requestBody, fullUrl, null);
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
        proxy(ctx, request, uri, requestBody, fullUrl, flow.id());
    }

    private void proxy(ChannelHandlerContext ctx,
                       FullHttpRequest request,
                       String uri,
                       byte[] requestBody,
                       String fullUrl,
                       String flowId) {
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(flowId == null
                                ? new HttpProxyBackendHandler(ctx.channel())
                                : new HttpProxyBackendHandler(ctx.channel(), sessionStore, flowId));
                    }
                });

        bootstrap.connect(targetHost, targetPort).addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                log.warn("Plain CONNECT upstream connect failed: {} - {}", fullUrl, summarize(connectFuture.cause()));
                if (flowId != null) {
                    sessionStore.fail(flowId, HttpResponseStatus.BAD_GATEWAY.code(),
                            connectFuture.cause() == null ? "Upstream connect failed" : summarize(connectFuture.cause()));
                }
                writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY,
                        connectFuture.cause() == null ? "Upstream connect failed" : summarize(connectFuture.cause()));
                return;
            }
            Channel upstreamChannel = ((io.netty.channel.ChannelFuture) connectFuture).channel();
            FullHttpRequest outboundRequest = buildOutboundRequest(request, uri, requestBody);
            upstreamChannel.writeAndFlush(outboundRequest).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    log.warn("Plain CONNECT upstream write failed: {} - {}", fullUrl, summarize(writeFuture.cause()));
                    if (flowId != null) {
                        sessionStore.fail(flowId, HttpResponseStatus.BAD_GATEWAY.code(),
                                writeFuture.cause() == null ? "Failed to send upstream request" : summarize(writeFuture.cause()));
                    }
                    writeSimpleResponse(ctx, HttpResponseStatus.BAD_GATEWAY, "Failed to send upstream request");
                    upstreamChannel.close();
                }
            });
        });
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
        outbound.headers().set(HttpHeaderNames.HOST, targetPort == 80 ? targetHost : targetHost + ":" + targetPort);
        if (!webSocketUpgrade) {
            outbound.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        if (requestBody.length > 0 || !webSocketUpgrade) {
            HttpUtil.setContentLength(outbound, requestBody.length);
        }
        return outbound;
    }

    private boolean isWebSocketUpgradeRequest(FullHttpRequest request) {
        return "websocket".equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE));
    }

    private CaptureSourceInfo sourceInfoForFilter(CaptureRequestFilter captureFilter) {
        if (captureFilter != null && captureFilter.requiresResolvedSource()) {
            return connectionContext.awaitSourceInfo(SOURCE_FILTER_RESOLVE_TIMEOUT);
        }
        return connectionContext.sourceInfo();
    }

    private Map<String, String> flattenHeaders(io.netty.handler.codec.http.HttpHeaders headers) {
        Map<String, String> flattened = new LinkedHashMap<>();
        headers.forEach(entry -> flattened.put(entry.getKey(), entry.getValue()));
        return flattened;
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
