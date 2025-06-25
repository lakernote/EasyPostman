package com.laker.postman.util;

import lombok.Data;
import okhttp3.Handshake;
import okhttp3.Protocol;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集 HTTP 全流程事件信息
 */
@Data
public class HttpEventInfo {
    // 连接信息
    private String localAddress;
    private String remoteAddress;
    // 各阶段时间戳
    private long callStart;
    private long dnsStart;
    private long dnsEnd;
    private long connectStart;
    private long connectEnd;
    private long secureConnectStart;
    private long secureConnectEnd;
    private long requestHeadersStart;
    private long requestHeadersEnd;
    private long requestBodyStart;
    private long requestBodyEnd;
    private long responseHeadersStart;
    private long responseHeadersEnd;
    private long responseBodyStart;
    private long responseBodyEnd;
    private long callEnd;
    private long callFailed;
    // 协议
    private Protocol protocol;
    // TLS/证书
    private List<Certificate> peerCertificates = new ArrayList<>();
    private List<Certificate> localCertificates = new ArrayList<>();
    private String tlsVersion;
    // 异常
    private String errorMessage;
    private Throwable error;
    // 其他
    private String threadName;
    // 便于输出
    @Override
    public String toString() {
        return "HttpEventInfo{" +
                "localAddress='" + localAddress + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", callStart=" + callStart +
                ", dnsStart=" + dnsStart +
                ", dnsEnd=" + dnsEnd +
                ", connectStart=" + connectStart +
                ", connectEnd=" + connectEnd +
                ", secureConnectStart=" + secureConnectStart +
                ", secureConnectEnd=" + secureConnectEnd +
                ", requestHeadersStart=" + requestHeadersStart +
                ", requestHeadersEnd=" + requestHeadersEnd +
                ", requestBodyStart=" + requestBodyStart +
                ", requestBodyEnd=" + requestBodyEnd +
                ", responseHeadersStart=" + responseHeadersStart +
                ", responseHeadersEnd=" + responseHeadersEnd +
                ", responseBodyStart=" + responseBodyStart +
                ", responseBodyEnd=" + responseBodyEnd +
                ", callEnd=" + callEnd +
                ", callFailed=" + callFailed +
                ", protocol=" + protocol +
                ", tlsVersion='" + tlsVersion + '\'' +
                ", peerCertificates=" + peerCertificates +
                ", localCertificates=" + localCertificates +
                ", errorMessage='" + errorMessage + '\'' +
                ", threadName='" + threadName + '\'' +
                '}';
    }
}

