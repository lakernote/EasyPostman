package com.laker.postman.plugin.capture;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class HttpProxyBackendHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger log = LoggerFactory.getLogger(HttpProxyBackendHandler.class);

    private final Channel clientChannel;
    private final CaptureSessionStore sessionStore;
    private final String flowId;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private volatile boolean responseStarted;
    private volatile boolean switchedToTunnel;

    HttpProxyBackendHandler(Channel clientChannel, CaptureSessionStore sessionStore, String flowId) {
        this.clientChannel = clientChannel;
        this.sessionStore = sessionStore;
        this.flowId = flowId;
    }

    HttpProxyBackendHandler(Channel clientChannel) {
        this(clientChannel, null, null);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
        if (message instanceof HttpResponse response) {
            handleResponse(ctx, response);
            if (switchedToTunnel) {
                return;
            }
        }
        if (message instanceof HttpContent content && !switchedToTunnel) {
            handleContent(ctx, content);
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, HttpResponse response) {
        responseStarted = true;
        if (flowId != null) {
            sessionStore.recordResponseStart(
                    flowId,
                    response.status().code(),
                    response.status().reasonPhrase(),
                    flattenHeaders(response.headers())
            );
        }

        if (isWebSocketUpgrade(response)) {
            writeWebSocketHandshake(ctx, response);
            return;
        }

        DefaultHttpResponse clientResponse = new DefaultHttpResponse(response.protocolVersion(), response.status());
        response.headers().forEach(entry -> clientResponse.headers().add(entry.getKey(), entry.getValue()));
        clientChannel.writeAndFlush(clientResponse).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.warn("Failed to write response headers to client for flow {}", flowId);
                future.channel().close();
                ctx.close();
            }
        });
    }

    private void handleContent(ChannelHandlerContext ctx, HttpContent content) {
        byte[] bytes = ByteBufUtil.getBytes(content.content());
        if (flowId != null && bytes.length > 0) {
            sessionStore.appendResponseBody(flowId, bytes);
        }

        HttpContent clientContent;
        if (content instanceof LastHttpContent lastContent) {
            DefaultLastHttpContent lastClientContent = new DefaultLastHttpContent(Unpooled.wrappedBuffer(bytes));
            lastContent.trailingHeaders().forEach(entry -> lastClientContent.trailingHeaders().add(entry.getKey(), entry.getValue()));
            clientContent = lastClientContent;
        } else {
            clientContent = new DefaultHttpContent(Unpooled.wrappedBuffer(bytes));
        }

        boolean last = content instanceof LastHttpContent;
        clientChannel.writeAndFlush(clientContent).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.warn("Failed to write response body to client for flow {}", flowId);
                future.channel().close();
                ctx.close();
                return;
            }
            if (last) {
                finishFlow();
                future.channel().close();
                ctx.channel().close();
            }
        });
    }

    private void writeWebSocketHandshake(ChannelHandlerContext ctx, HttpResponse response) {
        DefaultHttpResponse handshake = new DefaultHttpResponse(response.protocolVersion(), response.status());
        response.headers().forEach(entry -> handshake.headers().add(entry.getKey(), entry.getValue()));
        clientChannel.writeAndFlush(handshake).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.warn("Failed to forward WebSocket handshake for flow {}", flowId);
                future.channel().close();
                ctx.close();
                return;
            }
            switchToTunnel(ctx);
        });
    }

    private void switchToTunnel(ChannelHandlerContext ctx) {
        switchedToTunnel = true;
        removeIfPresent(ctx.pipeline(), HttpClientCodec.class);
        removeIfPresent(ctx.pipeline(), HttpProxyBackendHandler.class);
        removeIfPresent(clientChannel.pipeline(), HttpServerCodec.class);
        removeIfPresent(clientChannel.pipeline(), HttpObjectAggregator.class);
        removeIfPresent(clientChannel.pipeline(), HttpProxyFrontendHandler.class);
        removeIfPresent(clientChannel.pipeline(), HttpsMitmFrontendHandler.class);

        if (flowId != null) {
            ctx.pipeline().addLast("captureRelayUpstream", new CaptureRelayHandler(clientChannel, sessionStore, flowId, false));
            clientChannel.pipeline().addLast("captureRelayClient", new CaptureRelayHandler(ctx.channel(), sessionStore, flowId, true));
        } else {
            ctx.pipeline().addLast("directRelayUpstream", new DirectRelayHandler(clientChannel));
            clientChannel.pipeline().addLast("directRelayClient", new DirectRelayHandler(ctx.channel()));
        }
        ctx.channel().read();
        clientChannel.read();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (switchedToTunnel) {
            finishFlow();
            return;
        }
        if (!finished.get() && clientChannel.isActive()) {
            if (responseStarted) {
                finishFlow();
                clientChannel.close();
                return;
            }
            if (flowId != null) {
                log.warn("Upstream connection closed before response for flow {}", flowId);
                sessionStore.fail(flowId, HttpResponseStatus.BAD_GATEWAY.code(), "Upstream connection closed");
            }
            writeGatewayError();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (finished.get()) {
            ctx.close();
            return;
        }
        if (switchedToTunnel || responseStarted) {
            finishFlow();
            clientChannel.close();
            ctx.close();
            return;
        }
        if (flowId != null) {
            log.warn("Backend proxy error for flow {}: {}", flowId, summarize(cause), cause);
            sessionStore.fail(flowId, HttpResponseStatus.BAD_GATEWAY.code(),
                    cause == null ? "Upstream proxy error" : summarize(cause));
        }
        writeGatewayError();
        ctx.close();
    }

    private void finishFlow() {
        if (flowId != null && finished.compareAndSet(false, true)) {
            sessionStore.complete(flowId);
        }
    }

    private void writeGatewayError() {
        if (!clientChannel.isActive()) {
            return;
        }
        byte[] body = "Bad Gateway".getBytes(StandardCharsets.UTF_8);
        DefaultLastHttpContent content = new DefaultLastHttpContent(Unpooled.wrappedBuffer(body));
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, body.length);
        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        clientChannel.write(response);
        clientChannel.writeAndFlush(content).addListener(ChannelFutureListener.CLOSE);
    }

    private static boolean isWebSocketUpgrade(HttpResponse response) {
        return response.status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code()
                && "websocket".equalsIgnoreCase(response.headers().get(HttpHeaderNames.UPGRADE));
    }

    private static void removeIfPresent(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerType) {
        if (pipeline.get(handlerType) != null) {
            pipeline.remove(handlerType);
        }
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

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> flattened = new LinkedHashMap<>();
        headers.forEach(entry -> flattened.put(entry.getKey(), entry.getValue()));
        return flattened;
    }
}
