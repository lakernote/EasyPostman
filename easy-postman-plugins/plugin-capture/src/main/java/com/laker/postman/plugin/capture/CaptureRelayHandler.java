package com.laker.postman.plugin.capture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

final class CaptureRelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel peerChannel;
    private final CaptureSessionStore sessionStore;
    private final String flowId;
    private final boolean requestSide;
    private final WebSocketFramePreviewDecoder framePreviewDecoder = new WebSocketFramePreviewDecoder();

    CaptureRelayHandler(Channel peerChannel,
                        CaptureSessionStore sessionStore,
                        String flowId,
                        boolean requestSide) {
        this.peerChannel = peerChannel;
        this.sessionStore = sessionStore;
        this.flowId = flowId;
        this.requestSide = requestSide;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!peerChannel.isActive()) {
            ctx.close();
            return;
        }
        byte[] bytes = extractBytes(msg);
        if (bytes.length > 0) {
            if (requestSide) {
                sessionStore.appendRequestBody(flowId, bytes);
                framePreviewDecoder.decode(bytes).forEach(event -> sessionStore.appendRequestStreamEvent(flowId, event));
            } else {
                sessionStore.appendResponseBody(flowId, bytes);
                framePreviewDecoder.decode(bytes).forEach(event -> sessionStore.appendResponseStreamEvent(flowId, event));
            }
        }
        peerChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
            } else {
                future.channel().close();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        sessionStore.complete(flowId);
        if (peerChannel.isActive()) {
            peerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private byte[] extractBytes(Object msg) {
        if (msg instanceof ByteBuf byteBuf) {
            return ByteBufUtil.getBytes(byteBuf);
        }
        if (msg instanceof WebSocketFrame frame) {
            return ByteBufUtil.getBytes(frame.content());
        }
        return new byte[0];
    }
}
