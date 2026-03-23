package com.laker.postman.plugin.capture;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

final class CaptureProxyService {
    private final CaptureSessionStore sessionStore = new CaptureSessionStore();
    private final CaptureCertificateService certificateService = new CaptureCertificateService();
    private final SystemProxyService systemProxyService = new SystemProxyService();

    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;
    private volatile String listenHost = "127.0.0.1";
    private volatile int listenPort = 8888;
    private volatile boolean syncSystemProxy;
    private volatile CaptureHostFilter captureHostFilter = CaptureHostFilter.parse("");

    CaptureSessionStore sessionStore() {
        return sessionStore;
    }

    synchronized void start(String host, int port, boolean syncSystemProxy, String captureHostFilterText) throws Exception {
        if (isRunning()) {
            return;
        }
        listenHost = host;
        listenPort = port;
        this.syncSystemProxy = syncSystemProxy;
        captureHostFilter = CaptureHostFilter.parse(captureHostFilterText);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new CaptureServerInitializer(sessionStore, certificateService, captureHostFilter));
            serverChannel = bootstrap.bind(listenHost, listenPort).sync().channel();
            if (syncSystemProxy) {
                systemProxyService.enable(listenHost, listenPort);
            }
        } catch (Exception ex) {
            stop();
            throw ex;
        }
    }

    synchronized void stop() {
        RuntimeException restoreError = null;
        try {
            if (systemProxyService.isActive()) {
                systemProxyService.disable();
            }
        } catch (Exception ex) {
            restoreError = new IllegalStateException("Failed to restore system proxy: " + ex.getMessage(), ex);
        } finally {
            syncSystemProxy = false;
        }
        Channel channel = serverChannel;
        serverChannel = null;
        if (channel != null) {
            channel.close().awaitUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().awaitUninterruptibly();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().awaitUninterruptibly();
            workerGroup = null;
        }
        if (restoreError != null) {
            throw restoreError;
        }
    }

    boolean isRunning() {
        Channel channel = serverChannel;
        return channel != null && channel.isActive();
    }

    String listenHost() {
        return listenHost;
    }

    int listenPort() {
        return listenPort;
    }

    String rootCertificatePath() throws Exception {
        return certificateService.rootCertificatePath();
    }

    boolean isSystemProxySyncSupported() {
        return systemProxyService.isSupported();
    }

    boolean isSystemProxySynced() {
        return systemProxyService.isActive();
    }

    boolean syncSystemProxy() {
        return syncSystemProxy;
    }

    String systemProxyStatus() {
        return systemProxyService.statusSummary();
    }

    String captureFilterSummary() {
        return captureHostFilter.summary();
    }
}
