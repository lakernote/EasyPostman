package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.OkHttpRequestSnapshotCapture;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

final class RequestSnapshotNetworkInterceptor implements Interceptor {
    private final PreparedRequest preparedRequest;
    private final boolean captureBody;

    RequestSnapshotNetworkInterceptor(PreparedRequest preparedRequest, boolean captureBody) {
        this.preparedRequest = preparedRequest;
        this.captureBody = captureBody;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // network interceptor 才能看到 OkHttp 实际使用的连接和补齐后的请求头。
        HttpExchangeTraceSupport.updateFromConnection(preparedRequest, chain.connection());
        OkHttpRequestSnapshotCapture.capture(preparedRequest, chain.request(), captureBody);
        return chain.proceed(chain.request());
    }
}
