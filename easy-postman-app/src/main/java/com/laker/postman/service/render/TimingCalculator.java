package com.laker.postman.service.render;

import com.laker.postman.model.HttpEventInfo;

// 时序计算辅助类
public class TimingCalculator {
    private final HttpEventInfo info;

    public TimingCalculator(HttpEventInfo info) {
        this.info = info;
    }

    public long getTotal() {
        return calculateDuration(info.getCallStart(), info.getCallEnd());
    }

    public long getQueueing() {
        return info.getQueueingCost() > 0 ? info.getQueueingCost() :
                calculateDuration(info.getQueueStart(), info.getCallStart());
    }

    public long getStalled() {
        return info.getStalledCost() > 0 ? info.getStalledCost() :
                calculateDuration(info.getCallStart(), info.getConnectStart());
    }

    public long getDns() {
        return calculateDuration(info.getDnsStart(), info.getDnsEnd());
    }

    public long getConnect() {
        return calculateDuration(info.getConnectStart(), info.getConnectEnd());
    }

    public long getTls() {
        return calculateDuration(info.getSecureConnectStart(), info.getSecureConnectEnd());
    }

    public long getRequestSent() {
        long reqHeaders = calculateDuration(info.getRequestHeadersStart(), info.getRequestHeadersEnd());
        long reqBody = calculateDuration(info.getRequestBodyStart(), info.getRequestBodyEnd());

        if (reqHeaders >= 0 && reqBody >= 0) {
            return reqHeaders + reqBody;
        } else if (reqHeaders >= 0) {
            return reqHeaders;
        } else if (reqBody >= 0) {
            return reqBody;
        }
        return -1;
    }

    public long getServerCost() {
        if (info.getResponseHeadersStart() <= 0) {
            return -1;
        }

        // 优先使用 RequestBodyEnd，如果没有则使用 RequestHeadersEnd
        long requestEndTime = info.getRequestBodyEnd() > 0 ?
                info.getRequestBodyEnd() : info.getRequestHeadersEnd();

        if (requestEndTime <= 0) {
            return -1;
        }

        return calculateDuration(requestEndTime, info.getResponseHeadersStart());
    }

    public long getResponseBody() {
        return calculateDuration(info.getResponseBodyStart(), info.getResponseBodyEnd());
    }

    public boolean getConnectionReused() {
        // 如果没有连接获取事件，无法判断
        if (info.getConnectionAcquired() <= 0) {
            return false;
        }

        // 如果连接获取时间早于连接开始时间，说明是复用的连接
        return info.getConnectStart() <= 0 || info.getConnectionAcquired() < info.getConnectStart();
    }

    private long calculateDuration(long start, long end) {
        if (start <= 0 || end <= 0 || end < start) {
            return -1;
        }

        long duration = end - start;

        // 防止异常大的时间差（超过1小时认为异常）
        if (duration > 3600000) { // 3600000ms = 1小时
            return -1;
        }

        return duration;
    }
}