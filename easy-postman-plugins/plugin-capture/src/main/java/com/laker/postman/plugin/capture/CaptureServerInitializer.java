package com.laker.postman.plugin.capture;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

final class CaptureServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MAX_HTTP_OBJECT_SIZE = 10 * 1024 * 1024;

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureRequestFilter captureRequestFilter;

    CaptureServerInitializer(CaptureSessionStore sessionStore,
                             CaptureCertificateService certificateService,
                             CaptureRequestFilter captureRequestFilter) {
        this.sessionStore = sessionStore;
        this.certificateService = certificateService;
        this.captureRequestFilter = captureRequestFilter;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        channel.pipeline().addLast(new HttpServerCodec());
        channel.pipeline().addLast(new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
        channel.pipeline().addLast(new HttpProxyFrontendHandler(sessionStore, certificateService, captureRequestFilter));
    }
}
