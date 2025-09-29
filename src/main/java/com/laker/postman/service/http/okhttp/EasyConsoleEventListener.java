package com.laker.postman.service.http.okhttp;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.collections.right.request.sub.NetworkLogPanel;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

/**
 * 事件监听器，既记录详细连接事件和耗时，也统计连接信息
 */
@Slf4j
public class EasyConsoleEventListener extends EventListener {
    private static final ThreadLocal<HttpEventInfo> eventInfoThreadLocal = new ThreadLocal<>();
    private final long callStartNanos;
    private final HttpEventInfo info;
    private String reqItemId;
    private RequestEditSubPanel editSubPanel;
    private PreparedRequest preparedRequest;

    public EasyConsoleEventListener(PreparedRequest preparedRequest) {
        this.callStartNanos = System.nanoTime();
        String threadName = Thread.currentThread().getName();
        this.info = new HttpEventInfo();
        info.setThreadName(threadName);
        eventInfoThreadLocal.set(info);
        this.preparedRequest = preparedRequest;
        this.reqItemId = preparedRequest.id;
    }

    private void log(String stage, String msg) {
        long now = System.nanoTime();
        long elapsedMs = (now - callStartNanos) / 1_000_000;
        try {
            Color logColor;
            boolean bold;
            switch (stage) {
                case "callFailed":
                case "requestFailed":
                case "responseFailed":
                case "connectFailed":
                    logColor = new Color(220, 53, 69);
                    bold = true;
                    break;
                case "connectStart":
                case "connectEnd":
                case "connectionAcquired":
                case "connectionReleased":
                    logColor = new Color(0, 123, 255);
                    bold = false;
                    break;
                case "secureConnectStart":
                case "secureConnectEnd":
                    logColor = new Color(111, 66, 193);
                    bold = false;
                    break;
                case "callStart":
                case "callEnd":
                    logColor = new Color(40, 167, 69);
                    bold = true;
                    break;
                case "responseHeadersEnd:redirect":
                    logColor = new Color(255, 165, 0); // 橙色
                    bold = true;
                    break;
                default:
                    logColor = new Color(33, 37, 41);
                    bold = false;
            }
            String logMsg = "[" + stage + "] +" + elapsedMs + "ms: " + msg;
            // 输出到 NetworkLogPanel
            try {
                if (editSubPanel == null) {
                    editSubPanel = SingletonFactory.getInstance(RequestEditPanel.class).getRequestEditSubPanel(reqItemId);
                }
                NetworkLogPanel netPanel = editSubPanel.getResponsePanel().getNetworkLogPanel();
                netPanel.appendLog(logMsg, logColor, bold);
            } catch (Exception ignore) {
            }
        } catch (Exception e) {
            // 防止日志异常影响主流程
        }
    }

    @Override
    public void callStart(Call call) {
        info.setCallStart(System.currentTimeMillis());
        Request request = call.request();
        log("callStart", request.method() + " " + request.url());
    }

    @Override
    public void proxySelectStart(Call call, HttpUrl url) {
        info.setProxySelectStart(System.currentTimeMillis());
        log("proxySelectStart", "Selecting proxy for " + url);
    }

    @Override
    public void proxySelectEnd(Call call, HttpUrl url, List<Proxy> proxies) {
        info.setProxySelectEnd(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        sb.append("Proxies: ");
        for (Proxy proxy : proxies) {
            sb.append(proxy.type()).append(" ");
            if (proxy.address() instanceof InetSocketAddress address) {
                sb.append(address.getHostName()).append(":").append(address.getPort()).append(" ");
            }
        }
        log("proxySelectEnd", sb.toString());
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        info.setDnsStart(System.currentTimeMillis());
        log("dnsStart", domainName);
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        info.setDnsEnd(System.currentTimeMillis());
        log("dnsEnd", domainName + " -> " + inetAddressList);
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        info.setConnectStart(System.currentTimeMillis());
        info.setRemoteAddress(inetSocketAddress.toString());
        log("connectStart", inetSocketAddress + " via " + proxy.type());
    }

    @Override
    public void secureConnectStart(Call call) {
        info.setSecureConnectStart(System.currentTimeMillis());
        log("secureConnectStart", "TLS handshake start");
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        info.setSecureConnectEnd(System.currentTimeMillis());
        if (handshake != null) {
            info.setTlsVersion(handshake.tlsVersion().javaName());
            info.setCipherName(handshake.cipherSuite().toString());
            info.setPeerCertificates(handshake.peerCertificates());
            info.setLocalCertificates(handshake.localCertificates());
        }
        // 记录handshake信息
        if (handshake != null) {
            StringBuilder handshakeInfo = new StringBuilder();
            handshakeInfo.append("SSL connection using ")
                    .append(handshake.tlsVersion())
                    .append(" / ")
                    .append(handshake.cipherSuite())
                    .append("\n");
            List<Certificate> peerCertificates = handshake.peerCertificates();
            if (!peerCertificates.isEmpty()) {
                Certificate cert = peerCertificates.get(0);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    handshakeInfo.append("Server certificate:\n");
                    handshakeInfo.append(" subject: ").append(x509.getSubjectDN()).append("\n");
                    handshakeInfo.append(" start date: ").append(x509.getNotBefore()).append(" GMT\n");
                    handshakeInfo.append(" expire date: ").append(x509.getNotAfter()).append(" GMT\n");
                    Collection<List<?>> altNames = null;
                    try {
                        altNames = x509.getSubjectAlternativeNames();
                    } catch (Exception ignored) {
                    }
                    if (altNames != null) {
                        handshakeInfo.append(" subjectAltName: ");
                        for (List<?> altName : altNames) {
                            if (altName.size() > 1) {
                                handshakeInfo.append(altName.get(1)).append(", ");
                            }
                        }
                        if (handshakeInfo.charAt(handshakeInfo.length() - 2) == ',') {
                            handshakeInfo.setLength(handshakeInfo.length() - 2);
                        }
                        handshakeInfo.append("\n");
                    }
                    handshakeInfo.append(" issuer: ").append(x509.getIssuerDN()).append("\n");
                }
            }
            handshakeInfo.append("SSL certificate verify ok.\n");
            log("secureConnectEnd", handshakeInfo.toString());
        } else {
            log("secureConnectEnd", "no handshake");
        }
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        info.setConnectEnd(System.currentTimeMillis());
        info.setProtocol(protocol);
        log("connectEnd", inetSocketAddress + " via " + proxy.type() + ", protocol=" + protocol);
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        info.setErrorMessage(ioe.getMessage());
        info.setError(ioe);
        log("connectFailed", inetSocketAddress + " via " + proxy.type() + ", protocol=" + protocol + ", error: " + ioe.getMessage());
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        info.setConnectionAcquired(System.currentTimeMillis());
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
        log("connectionAcquired", "Connection acquired: " + connection.toString() + ", local=" + info.getLocalAddress() + ", remote=" + info.getRemoteAddress());
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        info.setConnectionReleased(System.currentTimeMillis());
        log("connectionReleased", "Connection released: " + connection.toString() + ", local=" + info.getLocalAddress() + ", remote=" + info.getRemoteAddress());
    }

    @Override
    public void requestHeadersStart(Call call) {
        info.setRequestHeadersStart(System.currentTimeMillis());
        log("requestHeadersStart", "");
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        info.setHeaderBytesSent(request.headers().toString().getBytes().length);
        info.setRequestHeadersEnd(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        Headers headers = request.headers();
        preparedRequest.okHttpHeaders = headers;
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            if (name.equalsIgnoreCase("cookie")) {
                // 只保留可见字符，避免乱码
                value = value.replaceAll("[^\\x20-\\x7E]", "");
            }
            sb.append(name).append(": ").append(value).append("\n");
        }
        log("requestHeadersEnd", sb.toString());
    }

    @Override
    public void requestBodyStart(Call call) {
        info.setRequestBodyStart(System.currentTimeMillis());
        Request request = call.request();
        if (request.body() != null) {
            MediaType contentType = request.body().contentType();
            String desc = null;
            String type = null;
            if (contentType != null) {
                type = contentType.type().toLowerCase();
                String subtype = contentType.subtype().toLowerCase();
                if (type.equals("multipart")) {
                    desc = "[multipart/form-data]";
                } else if (type.equals("application") && (subtype.contains("octet-stream") || subtype.contains("binary"))) {
                    desc = "[binary/octet-stream]";
                } else if (type.equals("image") || type.equals("audio") || type.equals("video")) {
                    desc = "[" + type + "/" + subtype + "]";
                }
            }
            if (desc != null) {
                // 尝试获取文件名
                if (type.equals("multipart")) {
                    desc = "[multipart/form-data] (see form files)";
                } else {
                    try {
                        if (request.body().contentLength() > 0 && request.body().getClass().getSimpleName().toLowerCase().contains("file")) {
                            desc += " (file upload)";
                        }
                    } catch (IOException ignored) {
                    }
                }
                preparedRequest.okHttpRequestBody = desc;
                log("requestBodyStart", desc);
            } else {
                try {
                    okio.Buffer buffer = new okio.Buffer();
                    request.body().writeTo(buffer);
                    String bodyString = buffer.readUtf8();
                    preparedRequest.okHttpRequestBody = bodyString;
                    if (!bodyString.isEmpty()) {
                        log("requestBodyStart", "\n" + bodyString);
                    } else {
                        log("requestBodyStart", "Request body is empty");
                    }
                } catch (Exception e) {
                    preparedRequest.okHttpRequestBody = "[读取请求体失败: " + e.getMessage() + "]";
                    log("requestBodyStart", "Failed to read request body: " + e.getMessage() + "\n" + getStackTrace(e));
                }
            }
        } else {
            preparedRequest.okHttpRequestBody = null;
            log("requestBodyStart", "No request body");
        }

    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        info.setBodyBytesSent(byteCount);
        info.setRequestBodyEnd(System.currentTimeMillis());
        log("requestBodyEnd", "bytes=" + byteCount);
    }


    @Override
    public void requestFailed(Call call, IOException ioe) {
        info.setErrorMessage(ioe.getMessage());
        info.setError(ioe);
        log("requestFailed", ioe.getMessage() + "\n" + getStackTrace(ioe));
    }

    @Override
    public void responseHeadersStart(Call call) {
        info.setResponseHeadersStart(System.currentTimeMillis());
        log("responseHeadersStart", "");
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        info.setHeaderBytesReceived(response.headers().toString().getBytes().length);
        info.setResponseHeadersEnd(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder("\n");
        boolean isRedirect = response.isRedirect();
        sb.append("Redirect: ").append(isRedirect).append("\n");
        sb.append("Response Code: ").append(response.code()).append(" ").append(response.message()).append("\n");
        sb.append("Protocol: ").append(response.protocol()).append("\n");
        sb.append("Content-Type: ").append(response.header("Content-Type", "")).append("\n");
        sb.append("Content-Length: ").append(response.header("Content-Length", "")).append("\n");
        if (isRedirect) {
            sb.append("Location: ").append(response.header("Location", "")).append("\n");
        }
        if (response.cacheResponse() != null) {
            sb.append("Cache: HIT\n");
        } else {
            sb.append("Cache: MISS\n");
        }
        if (response.networkResponse() != null) { // 如果有 networkResponse，说明是网络请求
            sb.append("Network: YES\n");
        } else {
            sb.append("Network: NO\n");
        }
        if (response.priorResponse() != null) { // 如果有 priorResponse，说明是重定向或缓存的响应
            sb.append("PriorResponse: YES\n");
        }
        sb.append("\n");
        sb.append("Headers:\n");
        // 处理响应头
        Headers headers = response.headers();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i);
            String value = headers.value(i);
            if (name.equalsIgnoreCase("set-cookie")) {
                // 只保留可见字符，避免乱码
                value = value.replaceAll("[^\\x20-\\x7E]", "");
            }
            sb.append(name).append(": ").append(value).append("\n");
        }
        // 如果是重定向，使用橙色高亮
        if (isRedirect) {
            log("responseHeadersEnd:redirect", sb.toString());
        } else {
            log("responseHeadersEnd", sb.toString());
        }
    }

    @Override
    public void responseBodyStart(Call call) {
        info.setResponseBodyStart(System.currentTimeMillis());
        log("responseBodyStart", "");
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        info.setBodyBytesReceived(byteCount);
        info.setResponseBodyEnd(System.currentTimeMillis());
        log("responseBodyEnd", "bytes=" + byteCount);

    }

    @Override
    public void responseFailed(Call call, IOException ioe) {
        info.setErrorMessage(ioe.getMessage());
        info.setError(ioe);
        log("responseFailed", ioe.getMessage() + "\n" + getStackTrace(ioe));
    }

    @Override
    public void callEnd(Call call) {
        info.setCallEnd(System.currentTimeMillis());
        log("callEnd", "done");
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        info.setCallFailed(System.currentTimeMillis());
        info.setErrorMessage(ioe.getMessage());
        info.setError(ioe);
        log("callFailed", ioe.getMessage());
    }

    @Override
    public void canceled(Call call) {
        info.setCanceled(System.currentTimeMillis());
        log("canceled", "Call was canceled");
    }


    @Override
    public void satisfactionFailure(Call call, Response response) {
        info.setErrorMessage("Response does not satisfy request: " + response.code() + " " + response.message());
        log("satisfactionFailure", "Response does not satisfy request: " + response.code() + " " + response.message());
    }


    @Override
    public void cacheHit(Call call, Response response) {
        log("cacheHit", "Response served from cache: " + response.code() + " " + response.message());
    }

    @Override
    public void cacheMiss(Call call) {
        log("cacheMiss", "No cache hit for this call");
    }

    @Override
    public void cacheConditionalHit(Call call, Response cachedResponse) {
        log("cacheConditionalHit", "Response served from conditional cache: " + cachedResponse.code() + " " + cachedResponse.message());
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
