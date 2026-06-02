package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSupport;
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
        // network interceptor 才能看到 OkHttp 实际使用的连接和补齐后的请求头。
        HttpExchangeTraceSupport.updateFromConnection(preparedRequest, chain.connection());
        OkHttpRequestSnapshotCapture.capture(preparedRequest, chain.request(), shouldCaptureBody());
        logRequestSnapshot();
        return chain.proceed(chain.request());
    }

    private boolean shouldCaptureBody() {
        return NetworkLogSupport.isEnabled(preparedRequest);
    }

    private void logRequestSnapshot() {
        if (!NetworkLogSupport.isEnabled(preparedRequest)) {
            return;
        }
        NetworkLogSupport.append(preparedRequest, NetworkLogEventStage.REQUEST_HEADERS_END, formatHeaders());
        NetworkLogSupport.append(preparedRequest, NetworkLogEventStage.REQUEST_BODY_START, formatBody());
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
