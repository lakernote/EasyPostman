package com.laker.postman.service.http.okhttp;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.panel.SidebarTabPanel;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.List;

/**
 * 事件监听器，既记录详细连接事件和耗时，也统计连接信息
 */
@Slf4j
public class EasyConsoleEventListener extends EventListener {
    private static final ThreadLocal<HttpEventInfo> eventInfoThreadLocal = new ThreadLocal<>();
    private final long callStartNanos;
    private final String threadName;
    private final HttpEventInfo info;

    public EasyConsoleEventListener() {
        this.callStartNanos = System.nanoTime();
        this.threadName = Thread.currentThread().getName();
        this.info = new HttpEventInfo();
        info.setThreadName(threadName);
        eventInfoThreadLocal.set(info);
    }

    private void log(String stage, String msg) {
        long now = System.nanoTime();
        long elapsedMs = (now - callStartNanos) / 1_000_000;
        try {
            if ("callStart".equals(stage)) {
                SidebarTabPanel.appendConsoleLog("\n—————————————— 新请求 ——————————————", SidebarTabPanel.LogType.CUSTOM);
            }
            boolean isErrorStage = "callFailed".equals(stage) || "requestFailed".equals(stage) || "responseFailed".equals(stage) || "connectFailed".equals(stage);
            if (isErrorStage && msg != null && msg.contains("at ")) {
                SidebarTabPanel.appendConsoleLog("———— 异常堆栈 ————", SidebarTabPanel.LogType.CUSTOM);
            }
            SidebarTabPanel.LogType logType;
            switch (stage) {
                case "callFailed":
                case "requestFailed":
                case "responseFailed":
                case "connectFailed":
                    logType = SidebarTabPanel.LogType.ERROR;
                    break;
                case "connectStart":
                case "connectEnd":
                case "connectionAcquired":
                case "connectionReleased":
                    logType = SidebarTabPanel.LogType.DEBUG;
                    break;
                case "secureConnectStart":
                case "secureConnectEnd":
                    logType = SidebarTabPanel.LogType.TRACE;
                    break;
                case "callEnd":
                    logType = SidebarTabPanel.LogType.SUCCESS;
                    break;
                default:
                    logType = SidebarTabPanel.LogType.INFO;
            }
            SidebarTabPanel.appendConsoleLog("[HTTP Event] [" + threadName + "] [" + stage + "] +" + elapsedMs + "ms: " + msg, logType);
        } catch (Exception e) {
            // 防止日志异常影响主流程
        }
    }

    @Override
    public void callStart(@NotNull Call call) {
        info.setCallStart(System.currentTimeMillis());
        log("callStart", call.request().method() + " " + call.request().url());
    }

    @Override
    public void dnsStart(@NotNull Call call, String domainName) {
        info.setDnsStart(System.currentTimeMillis());
        log("dnsStart", domainName);
    }

    @Override
    public void dnsEnd(@NotNull Call call, String domainName, List<InetAddress> inetAddressList) {
        info.setDnsEnd(System.currentTimeMillis());
        log("dnsEnd", domainName + " -> " + inetAddressList);
    }

    @Override
    public void connectStart(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy) {
        info.setConnectStart(System.currentTimeMillis());
        info.setRemoteAddress(inetSocketAddress.toString());
        log("connectStart", inetSocketAddress + " via " + proxy.type());
    }

    @Override
    public void secureConnectStart(@NotNull Call call) {
        info.setSecureConnectStart(System.currentTimeMillis());
        log("secureConnectStart", "TLS handshake start");
    }

    @Override
    public void secureConnectEnd(@NotNull Call call, Handshake handshake) {
        info.setSecureConnectEnd(System.currentTimeMillis());
        if (handshake != null) {
            info.setTlsVersion(handshake.tlsVersion().javaName());
            info.setPeerCertificates(handshake.peerCertificates());
            info.setLocalCertificates(handshake.localCertificates());
        }
        // 记录handshake信息
        if (handshake != null) {
            StringBuilder handshakeInfo = new StringBuilder();
            handshakeInfo.append("TLS version: ").append(handshake.tlsVersion()).append(", ");
            handshakeInfo.append("Cipher suite: ").append(handshake.cipherSuite()).append(", ");
            List<Certificate> peerCertificates = handshake.peerCertificates();
            handshakeInfo.append("Peer certificates: ").append(peerCertificates.size()).append(", ");
            List<Certificate> localCertificates = handshake.localCertificates();
            handshakeInfo.append("Local certificates: ").append(localCertificates.size());
            log("secureConnectEnd", handshakeInfo.toString());
        } else {
            log("secureConnectEnd", "no handshake");
        }
    }

    @Override
    public void connectEnd(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy, Protocol protocol) {
        info.setConnectEnd(System.currentTimeMillis());
        info.setProtocol(protocol);
        log("connectEnd", inetSocketAddress + " via " + proxy.type() + ", protocol=" + protocol);
    }

    @Override
    public void connectFailed(@NotNull Call call, @NotNull InetSocketAddress inetSocketAddress, @NotNull Proxy proxy, Protocol protocol, IOException ioe) {
        log("connectFailed", ioe.getMessage());
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
        log("connectionAcquired", connection.toString());
    }

    @Override
    public void connectionReleased(@NotNull Call call, @NotNull Connection connection) {
        log("connectionReleased", connection.toString());
    }

    @Override
    public void requestHeadersStart(@NotNull Call call) {
        info.setRequestHeadersStart(System.currentTimeMillis());
        log("requestHeadersStart", "");
    }

    @Override
    public void requestHeadersEnd(@NotNull Call call, @NotNull Request request) {
        info.setRequestHeadersEnd(System.currentTimeMillis());
        log("requestHeadersEnd", "");
    }

    @Override
    public void requestBodyStart(@NotNull Call call) {
        info.setRequestBodyStart(System.currentTimeMillis());
        log("requestBodyStart", "");
    }

    @Override
    public void requestBodyEnd(@NotNull Call call, long byteCount) {
        info.setRequestBodyEnd(System.currentTimeMillis());
        log("requestBodyEnd", "bytes=" + byteCount);
    }

    @Override
    public void responseHeadersStart(@NotNull Call call) {
        info.setResponseHeadersStart(System.currentTimeMillis());
        log("responseHeadersStart", "");
    }

    @Override
    public void responseHeadersEnd(@NotNull Call call, @NotNull Response response) {
        info.setResponseHeadersEnd(System.currentTimeMillis());
        log("responseHeadersEnd", "code=" + response.code());
    }

    @Override
    public void responseBodyStart(@NotNull Call call) {
        info.setResponseBodyStart(System.currentTimeMillis());
        log("responseBodyStart", "");
    }

    @Override
    public void responseBodyEnd(@NotNull Call call, long byteCount) {
        info.setResponseBodyEnd(System.currentTimeMillis());
        log("responseBodyEnd", "bytes=" + byteCount);
    }

    @Override
    public void callEnd(@NotNull Call call) {
        info.setCallEnd(System.currentTimeMillis());
        log("callEnd", "done");
    }

    @Override
    public void callFailed(@NotNull Call call, @NotNull IOException ioe) {
        info.setCallFailed(System.currentTimeMillis());
        info.setErrorMessage(ioe.getMessage());
        info.setError(ioe);
        log("callFailed", ioe.getMessage());
    }

    @Override
    public void requestFailed(@NotNull Call call, @NotNull IOException ioe) {
        log("requestFailed", ioe.getMessage() + "\n" + getStackTrace(ioe));
    }

    @Override
    public void responseFailed(@NotNull Call call, @NotNull IOException ioe) {
        log("responseFailed", ioe.getMessage() + "\n" + getStackTrace(ioe));
    }

    // 辅助方法：获取异常堆栈
    private String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append("    at ").append(e.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取并移除当前线程的 HttpEventInfo
     */
    public static HttpEventInfo getAndRemove() {
        HttpEventInfo info = eventInfoThreadLocal.get();
        eventInfoThreadLocal.remove();
        return info;
    }
}