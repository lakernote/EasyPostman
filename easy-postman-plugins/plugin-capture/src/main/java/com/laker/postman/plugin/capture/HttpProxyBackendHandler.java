package com.laker.postman.plugin.capture;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class HttpProxyBackendHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private static final Logger log = LoggerFactory.getLogger(HttpProxyBackendHandler.class);
    private final Channel clientChannel;
    private final CaptureSessionStore sessionStore;
    private final String flowId;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    HttpProxyBackendHandler(Channel clientChannel, CaptureSessionStore sessionStore, String flowId) {
        this.clientChannel = clientChannel;
        this.sessionStore = sessionStore;
        this.flowId = flowId;
    }

    HttpProxyBackendHandler(Channel clientChannel) {
        this(clientChannel, null, null);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
        finished.set(true);
        byte[] responseBody = ByteBufUtil.getBytes(response.content());
        if (flowId != null) {
            sessionStore.complete(flowId,
                    response.status().code(),
                    response.status().reasonPhrase(),
                    flattenHeaders(response.headers()),
                    responseBody);
        }

        FullHttpResponse clientResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                response.status(),
                Unpooled.wrappedBuffer(responseBody)
        );
        response.headers().forEach(entry -> {
            if (HttpHeaderNames.TRANSFER_ENCODING.contentEqualsIgnoreCase(entry.getKey())
                    || HttpHeaderNames.CONNECTION.contentEqualsIgnoreCase(entry.getKey())) {
                return;
            }
            clientResponse.headers().add(entry.getKey(), entry.getValue());
        });
        clientResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        HttpUtil.setContentLength(clientResponse, responseBody.length);
        clientChannel.writeAndFlush(clientResponse).addListener(ChannelFutureListener.CLOSE);
        ctx.channel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (!finished.get() && clientChannel.isActive()) {
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
        if (flowId != null) {
            log.warn("Backend proxy error for flow {}: {}", flowId, summarize(cause), cause);
            sessionStore.fail(flowId, HttpResponseStatus.BAD_GATEWAY.code(),
                    cause == null ? "Upstream proxy error" : summarize(cause));
        }
        writeGatewayError();
        ctx.close();
    }

    private void writeGatewayError() {
        if (!clientChannel.isActive()) {
            return;
        }
        byte[] body = "Bad Gateway".getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_GATEWAY,
                Unpooled.wrappedBuffer(body)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        HttpUtil.setContentLength(response, body.length);
        clientChannel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
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
