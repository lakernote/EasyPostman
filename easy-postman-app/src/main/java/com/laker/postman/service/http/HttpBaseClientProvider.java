package com.laker.postman.service.http;

import com.laker.postman.model.PreparedRequest;
import okhttp3.OkHttpClient;

@FunctionalInterface
public interface HttpBaseClientProvider {
    OkHttpClient getBaseClient(PreparedRequest request);
}
