package com.laker.postman.model;

import lombok.Data;
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
    private long queueStart; // newCall前的时间戳 自己额外定义的发起请求时间
    private long callStart;
    private long proxySelectStart;
    private long proxySelectEnd;
    private long dnsStart;
    private long dnsEnd;
    private long connectStart;
    private long secureConnectStart;
    private long secureConnectEnd;
    private long connectEnd;
    private long connectionAcquired;
    private long requestHeadersStart;
    private long requestHeadersEnd;
    private long requestBodyStart;
    private long requestBodyEnd;
    private long responseHeadersStart;
    private long responseHeadersEnd;
    private long responseBodyStart;
    private long responseBodyEnd;
    private long connectionReleased;
    private long callEnd;
    private long callFailed;
    private long canceled;

    // 耗时统计
    private long queueingCost; // 排队耗时
    private long stalledCost; // 阻塞耗时
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
}