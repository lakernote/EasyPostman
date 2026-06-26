package com.laker.postman.plugin.capture;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.RequiredArgsConstructor;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

@RequiredArgsConstructor
final class CaptureServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MAX_HTTP_OBJECT_SIZE = 10 * 1024 * 1024;

    private final CaptureSessionStore sessionStore;
    private final CaptureCertificateService certificateService;
    private final CaptureFilterState captureFilterState;
    private final CaptureSourceAppResolver sourceAppResolver;

    @Override
    protected void initChannel(SocketChannel channel) {
        CaptureConnectionContext connectionContext = CaptureConnectionContext.from(channel);
        connectionContext.addDiagnostic(CaptureDiagnosticEvent.info(
                CaptureDiagnosticPhase.INBOUND_ACCEPT,
                CaptureDiagnosticRole.CLIENT_CONNECTION,
                t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_ACCEPTED_CLIENT_CONNECTION),
                connectionContext.sourceInfo().detailText(),
                ""
        ));
        sourceAppResolver.resolveAsync(connectionContext, sessionStore);
        channel.pipeline().addLast(new HttpServerCodec());
        channel.pipeline().addLast(new HttpObjectAggregator(MAX_HTTP_OBJECT_SIZE));
        channel.pipeline().addLast(new HttpProxyFrontendHandler(sessionStore, certificateService, captureFilterState, connectionContext));
    }
}
