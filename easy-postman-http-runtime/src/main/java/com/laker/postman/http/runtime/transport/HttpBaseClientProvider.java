package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import okhttp3.OkHttpClient;

@FunctionalInterface
public interface HttpBaseClientProvider {
    OkHttpClient getBaseClient(PreparedRequest request);
}
