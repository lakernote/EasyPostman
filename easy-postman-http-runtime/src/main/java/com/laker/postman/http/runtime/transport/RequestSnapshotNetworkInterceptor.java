package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSink;
import com.laker.postman.http.runtime.okhttp.OkHttpRequestSnapshotCapture;
import com.laker.postman.request.model.HttpHeader;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

final class RequestSnapshotNetworkInterceptor implements Interceptor {
    private final PreparedRequest preparedRequest;

    RequestSnapshotNetworkInterceptor(PreparedRequest preparedRequest) {
        this.preparedRequest = preparedRequest;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        OkHttpRequestSnapshotCapture.capture(preparedRequest, chain.request(), shouldCaptureBody());
        logRequestSnapshot();
        return chain.proceed(chain.request());
    }

    private boolean shouldCaptureBody() {
        return preparedRequest != null && preparedRequest.enableNetworkLog;
    }

    private void logRequestSnapshot() {
        if (preparedRequest == null || !preparedRequest.enableNetworkLog) {
            return;
        }
        NetworkLogSink sink = preparedRequest.networkLogSink == null
                ? NetworkLogSink.noop()
                : preparedRequest.networkLogSink;
        try {
            sink.append(NetworkLogEventStage.REQUEST_HEADERS_END, formatHeaders(), null);
            sink.append(NetworkLogEventStage.REQUEST_BODY_START, formatBody(), null);
        } catch (Throwable ignored) {
            // Snapshot logging must not affect request execution.
        }
    }

    private String formatHeaders() {
        if (preparedRequest.sentHeadersList == null || preparedRequest.sentHeadersList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n");
        for (HttpHeader header : preparedRequest.sentHeadersList) {
            if (header == null || header.getKey() == null) {
                continue;
            }
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String formatBody() {
        if (preparedRequest.sentRequestBody == null) {
            return "No request body";
        }
        if (preparedRequest.sentRequestBody.isEmpty()) {
            return "Request body is empty";
        }
        return "\n" + preparedRequest.sentRequestBody;
    }
}
