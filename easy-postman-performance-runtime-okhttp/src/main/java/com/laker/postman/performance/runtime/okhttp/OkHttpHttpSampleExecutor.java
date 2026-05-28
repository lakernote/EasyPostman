package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import okhttp3.Call;
import okhttp3.Response;

final class OkHttpHttpSampleExecutor {
    private final OkHttpClientResolver clientResolver;
    private final OkHttpRequestFactory requestFactory;
    private final OkHttpActiveRequestRegistry activeRequests;

    OkHttpHttpSampleExecutor(OkHttpClientResolver clientResolver,
                             OkHttpRequestFactory requestFactory,
                             OkHttpActiveRequestRegistry activeRequests) {
        this.clientResolver = clientResolver;
        this.requestFactory = requestFactory;
        this.activeRequests = activeRequests;
    }

    PerformanceSampleRecord execute(PerformanceOutboundRequest request, long startTimeMs) throws Exception {
        Call call = clientResolver.clientFor(request).newCall(requestFactory.build(request));
        activeRequests.addHttpCall(call);
        try (Response response = call.execute()) {
            return OkHttpSampleRecords.httpRecord(request, response, startTimeMs);
        } finally {
            activeRequests.removeHttpCall(call);
        }
    }
}
