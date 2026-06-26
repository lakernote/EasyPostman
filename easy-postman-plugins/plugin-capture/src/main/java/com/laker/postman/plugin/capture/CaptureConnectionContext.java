package com.laker.postman.plugin.capture;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class CaptureConnectionContext {
    private final String connectionId;
    private final CopyOnWriteArrayList<CaptureDiagnosticEvent> diagnosticEvents = new CopyOnWriteArrayList<>();
    private final CompletableFuture<CaptureSourceInfo> sourceResolution = new CompletableFuture<>();
    private volatile CaptureSourceInfo sourceInfo;

    private CaptureConnectionContext(String connectionId, CaptureSourceInfo sourceInfo) {
        this.connectionId = connectionId;
        this.sourceInfo = sourceInfo == null ? CaptureSourceInfo.unknown() : sourceInfo;
    }

    static CaptureConnectionContext from(Channel channel) {
        return new CaptureConnectionContext(UUID.randomUUID().toString(), sourceInfoFrom(channel));
    }

    static CaptureConnectionContext forTest(String connectionId, CaptureSourceInfo sourceInfo) {
        return new CaptureConnectionContext(connectionId, sourceInfo);
    }

    String connectionId() {
        return connectionId;
    }

    CaptureSourceInfo sourceInfo() {
        return sourceInfo;
    }

    void updateSourceInfo(CaptureSourceInfo sourceInfo) {
        if (sourceInfo != null) {
            this.sourceInfo = sourceInfo;
        }
    }

    void completeSourceResolution(CaptureSourceInfo sourceInfo) {
        updateSourceInfo(sourceInfo);
        sourceResolution.complete(this.sourceInfo);
    }

    void completeSourceResolutionFailure() {
        sourceResolution.complete(sourceInfo);
    }

    CaptureSourceInfo awaitSourceInfo(Duration timeout) {
        if (hasResolvedProcess(sourceInfo) || sourceResolution.isDone()) {
            return sourceResolution.getNow(sourceInfo);
        }
        try {
            return sourceResolution.get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return sourceInfo;
        } catch (ExecutionException | TimeoutException ex) {
            return sourceInfo;
        }
    }

    void addDiagnostic(CaptureDiagnosticEvent event) {
        if (event != null) {
            diagnosticEvents.add(event);
        }
    }

    List<CaptureDiagnosticEvent> diagnosticSnapshot() {
        return new ArrayList<>(diagnosticEvents);
    }

    private static CaptureSourceInfo sourceInfoFrom(Channel channel) {
        if (channel == null) {
            return CaptureSourceInfo.unknown();
        }
        Endpoint remote = endpoint(channel.remoteAddress());
        Endpoint local = endpoint(channel.localAddress());
        return CaptureSourceInfo.network(remote.host(), remote.port(), local.host(), local.port());
    }

    private static Endpoint endpoint(SocketAddress address) {
        if (address instanceof InetSocketAddress inet) {
            String host = inet.getAddress() == null ? inet.getHostString() : inet.getAddress().getHostAddress();
            return new Endpoint(host, inet.getPort());
        }
        return new Endpoint("", 0);
    }

    private static boolean hasResolvedProcess(CaptureSourceInfo sourceInfo) {
        return sourceInfo != null
                && (!sourceInfo.processId().isBlank()
                || !sourceInfo.processName().isBlank()
                || !sourceInfo.processPath().isBlank());
    }

    private record Endpoint(String host, int port) {
    }
}
