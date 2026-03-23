package com.laker.postman.plugin.capture;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

final class CaptureServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MAX_HTTP_OBJECT_SIZE = 10 * 1024 * 1024;

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureHostFilter captureHostFilter;

    CaptureServerInitializer(CaptureSessionStore sessionStore,
                             CaptureCertificateService certificateService,
                             CaptureHostFilter captureHostFilter) {
        this.sessionStore = sessionStore;
        this.certificateService = certificateService;
        this.captureHostFilter = captureHostFilter;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        channel.pipeline().addLast(new HttpServerCodec());
        channel.pipeline().addLast(new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
        channel.pipeline().addLast(new HttpProxyFrontendHandler(sessionStore, certificateService, captureHostFilter));
    }
}
