package com.laker.postman.http.runtime.transport;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RealtimeConnectionOptions {
    private static final RealtimeConnectionOptions DEFAULT = RealtimeConnectionOptions.builder().build();

    HttpBaseClientProvider baseClientProvider;
    @Builder.Default
    boolean lifecycleLoggingEnabled = true;

    public static RealtimeConnectionOptions defaults() {
        return DEFAULT;
    }
}
