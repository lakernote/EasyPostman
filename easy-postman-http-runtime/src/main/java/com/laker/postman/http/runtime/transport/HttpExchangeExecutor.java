package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.cookie.HttpCookieStore;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.OkHttpResponseHandler;
import com.laker.postman.http.runtime.sse.SseResponseCallback;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public final class HttpExchangeExecutor {
    private final HttpClientResolver clientResolver;

    public HttpExchangeExecutor(HttpClientResolver clientResolver) {
        this.clientResolver = clientResolver == null ? HttpClientResolver.DEFAULT : clientResolver;
    }

    public HttpResponse executeHttp(PreparedRequest request, SseResponseCallback callback) throws Exception {
        return executeHttp(request, callback, HttpCallTracker.NOOP, null);
    }

    public HttpResponse executeHttp(PreparedRequest request,
                                    SseResponseCallback callback,
                                    HttpCallTracker callTracker) throws Exception {
        return executeHttp(request, callback, callTracker, null);
    }

    public HttpResponse executeHttp(PreparedRequest request,
                                    SseResponseCallback callback,
                                    HttpCallTracker callTracker,
                                    HttpBaseClientProvider baseClientProvider) throws Exception {
        Request okRequest = PreparedOkHttpRequestFactory.build(request);
        return executeRequest(request, okRequest, callback, callTracker, baseClientProvider);
    }

    private HttpResponse executeRequest(PreparedRequest request,
                                        Request okRequest,
                                        SseResponseCallback callback,
                                        HttpCallTracker callTracker,
                                        HttpBaseClientProvider baseClientProvider) throws Exception {
        OkHttpClient client = clientResolver.resolveClient(request, baseClientProvider);
        Call call = client.newCall(okRequest);
        HttpCallTracker resolvedTracker = callTracker == null ? HttpCallTracker.NOOP : callTracker;
        resolvedTracker.onCallStarted(call);
        try {
            return callWithRequest(request, call, client, callback);
        } finally {
            resolvedTracker.onCallFinished(call);
        }
    }

    private HttpResponse callWithRequest(PreparedRequest request,
                                         Call call,
                                         OkHttpClient client,
                                         SseResponseCallback callback) throws IOException {
        long startTime = System.currentTimeMillis();
        HttpResponse httpResponse = new HttpResponse();
        ConnectionPool pool = client.connectionPool();
        httpResponse.idleConnectionCount = pool.idleConnectionCount();
        httpResponse.connectionCount = pool.connectionCount();
        Response okResponse;
        try {
            okResponse = call.execute();
        } finally {
            HttpTraceInfoAttacher.attachTraceInfo(httpResponse, startTime);
        }
        OkHttpResponseHandler.handleResponse(
                okResponse,
                httpResponse,
                callback,
                request.responseBodyMode,
                request.responseBodyPreviewLimitBytes,
                request.downloadProgressSinkFactory,
                request.responseSizeLimitWarningSink
        );
        httpResponse.endTime = HttpTraceInfoAttacher.resolveResponseReceivedEndTime(
                httpResponse,
                System.currentTimeMillis()
        );
        httpResponse.costMs = httpResponse.endTime - startTime;
        if (request.notifyCookieChanges) {
            HttpCookieStore.notifyCookieChanged();
        }
        return httpResponse;
    }
}
