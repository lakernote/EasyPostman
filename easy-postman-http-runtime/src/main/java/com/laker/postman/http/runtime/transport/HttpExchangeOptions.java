package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.sse.SseResponseCallback;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class HttpExchangeOptions {
    private static final HttpExchangeOptions DEFAULT = HttpExchangeOptions.builder().build();

    SseResponseCallback callback;
    @Builder.Default
    HttpCallTracker callTracker = HttpCallTracker.NOOP;
    HttpBaseClientProvider baseClientProvider;

    public static HttpExchangeOptions defaults() {
        return DEFAULT;
    }

    public HttpCallTracker resolvedCallTracker() {
        return callTracker == null ? HttpCallTracker.NOOP : callTracker;
    }
}
