package com.laker.postman.service.okhttp;

import com.laker.postman.model.HttpEventInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.List;

/**
 * 连接信息线程安全存储与事件监听
 */
@Slf4j
public class ConnectionInfoHolder {
    private static final ThreadLocal<HttpEventInfo> eventInfoThreadLocal = new ThreadLocal<>();

    public static class EasyEventListener extends EventListener {
        private HttpEventInfo info;

        public EasyEventListener() {
            info = new HttpEventInfo();
            info.setThreadName(Thread.currentThread().getName());
            eventInfoThreadLocal.set(info);
        }

        @Override
        public void callStart(@NotNull Call call) {
            info.setCallStart(System.currentTimeMillis());
        }

        @Override
        public void dnsStart(@NotNull Call call, String domainName) {
            info.setDnsStart(System.currentTimeMillis());
        }

        @Override
        public void dnsEnd(@NotNull Call call, String domainName, List<InetAddress> inetAddressList) {
            info.setDnsEnd(System.currentTimeMillis());
        }

        @Override
        public void connectStart(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy) {
            info.setConnectStart(System.currentTimeMillis());
            info.setRemoteAddress(inetSocketAddress.toString());
        }

        @Override
        public void secureConnectStart(@NotNull Call call) {
            info.setSecureConnectStart(System.currentTimeMillis());
        }

        @Override
        public void secureConnectEnd(@NotNull Call call, Handshake handshake) {
            info.setSecureConnectEnd(System.currentTimeMillis());
            if (handshake != null) {
                info.setTlsVersion(handshake.tlsVersion().javaName());  // 获取 TLS 版本
                info.setPeerCertificates(handshake.peerCertificates()); // 获取对端证书信息
                info.setLocalCertificates(handshake.localCertificates()); // 获取证书信息
            }
        }

        @Override
        public void connectEnd(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy, Protocol protocol) {
            info.setConnectEnd(System.currentTimeMillis());
            info.setProtocol(protocol);
        }

        @Override
        public void connectionAcquired(@NotNull Call call, @NotNull Connection connection) {
            try {
                Socket socket = connection.socket();
                String local = socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
                String remote = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                info.setLocalAddress(local);
                info.setRemoteAddress(remote);
            } catch (Exception e) {
                info.setLocalAddress("无法获取");
                info.setRemoteAddress("无法获取");
            }
        }

        @Override
        public void requestHeadersStart(@NotNull Call call) {
            info.setRequestHeadersStart(System.currentTimeMillis());
        }

        @Override
        public void requestHeadersEnd(@NotNull Call call, @NotNull Request request) {
            info.setRequestHeadersEnd(System.currentTimeMillis());
        }

        @Override
        public void requestBodyStart(@NotNull Call call) {
            info.setRequestBodyStart(System.currentTimeMillis());
        }

        @Override
        public void requestBodyEnd(@NotNull Call call, long byteCount) {
            info.setRequestBodyEnd(System.currentTimeMillis());
        }

        @Override
        public void responseHeadersStart(@NotNull Call call) {
            info.setResponseHeadersStart(System.currentTimeMillis());
        }

        @Override
        public void responseHeadersEnd(@NotNull Call call, @NotNull Response response) {
            info.setResponseHeadersEnd(System.currentTimeMillis());
        }

        @Override
        public void responseBodyStart(@NotNull Call call) {
            info.setResponseBodyStart(System.currentTimeMillis());
        }

        @Override
        public void responseBodyEnd(@NotNull Call call, long byteCount) {
            info.setResponseBodyEnd(System.currentTimeMillis());
        }

        @Override
        public void callEnd(@NotNull Call call) {
            info.setCallEnd(System.currentTimeMillis());
        }

        @Override
        public void callFailed(@NotNull Call call, @NotNull IOException ioe) {
            info.setCallFailed(System.currentTimeMillis());
            info.setErrorMessage(ioe.getMessage());
            info.setError(ioe);
        }
    }

    public static EventListener getEventListener() {
        return new EasyEventListener();
    }

    public static HttpEventInfo getAndRemove() {
        HttpEventInfo info = eventInfoThreadLocal.get();
        eventInfoThreadLocal.remove();
        return info;
    }
}