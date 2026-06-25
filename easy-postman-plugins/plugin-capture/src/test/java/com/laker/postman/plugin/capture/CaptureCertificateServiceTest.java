package com.laker.postman.plugin.capture;

import com.laker.postman.util.SystemUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class CaptureCertificateServiceTest {
    private String previousDataDirectory;
    private Path tempDataDirectory;

    @BeforeMethod
    public void setUpDataDirectory() throws Exception {
        previousDataDirectory = System.getProperty("easyPostman.data.dir");
        tempDataDirectory = Files.createTempDirectory("easy-postman-capture-cert-");
        System.setProperty("easyPostman.data.dir", tempDataDirectory.toString());
        SystemUtil.resetForTests();
    }

    @AfterMethod(alwaysRun = true)
    public void restoreDataDirectory() throws Exception {
        if (previousDataDirectory == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", previousDataDirectory);
        }
        SystemUtil.resetForTests();
        deleteRecursively(tempDataDirectory);
    }

    @Test(timeOut = 10000)
    public void shouldAcceptUntrustedUpstreamCertificatesDuringCaptureProxyTls() throws Exception {
        CaptureCertificateService service = new CaptureCertificateService();
        SslContext upstreamServerContext = service.buildServerSslContext("localhost");
        SslContext upstreamClientContext = service.buildClientSslContext();
        EventLoopGroup group = new NioEventLoopGroup(2);
        CompletableFuture<Void> serverHandshake = new CompletableFuture<>();
        CompletableFuture<Void> clientHandshake = new CompletableFuture<>();
        Channel serverChannel = null;
        Channel clientChannel = null;

        try {
            serverChannel = new ServerBootstrap()
                    .group(group, group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            SslHandler sslHandler = upstreamServerContext.newHandler(ch.alloc());
                            sslHandler.handshakeFuture().addListener(future -> completeHandshake(serverHandshake, future.cause()));
                            ch.pipeline().addLast(sslHandler);
                            ch.pipeline().addLast(new NoopInboundHandler());
                        }
                    })
                    .bind("127.0.0.1", 0)
                    .sync()
                    .channel();

            int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();
            clientChannel = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            SslHandler sslHandler = upstreamClientContext.newHandler(ch.alloc(), "localhost", port);
                            sslHandler.handshakeFuture().addListener(future -> completeHandshake(clientHandshake, future.cause()));
                            ch.pipeline().addLast(sslHandler);
                        }
                    })
                    .connect("127.0.0.1", port)
                    .sync()
                    .channel();

            clientHandshake.get(5, TimeUnit.SECONDS);
            serverHandshake.get(5, TimeUnit.SECONDS);
        } finally {
            if (clientChannel != null) {
                clientChannel.close().sync();
            }
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            group.shutdownGracefully().sync();
        }
    }

    private static void completeHandshake(CompletableFuture<Void> handshake, Throwable cause) {
        if (cause == null) {
            handshake.complete(null);
        } else {
            handshake.completeExceptionally(cause);
        }
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path current : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }

    private static final class NoopInboundHandler extends SimpleChannelInboundHandler<Object> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        }
    }
}
