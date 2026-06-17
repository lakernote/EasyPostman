package com.laker.postman.history;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import lombok.Getter;

/**
 * 请求历史项，包含请求和响应的简要信息
 */
@Getter
public class RequestHistoryItem {
    private final String method;
    private final String url;
    private final int responseCode;
    private final long requestTime;
    private final PreparedRequest request;
    private final HttpResponse response;

    public RequestHistoryItem(PreparedRequest request, HttpResponse response, long requestTime) {
        this.method = request.method;
        this.url = request.url;
        this.responseCode = response.code;
        this.request = request;
        this.response = response;
        this.requestTime = requestTime;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", method, url);
    }
}
