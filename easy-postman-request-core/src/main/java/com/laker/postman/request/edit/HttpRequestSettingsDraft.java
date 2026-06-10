package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpRequestVersions;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpRequestSettingsDraft {
    Boolean followRedirects;
    Boolean cookieJarEnabled;
    @Builder.Default
    HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT;
    @Builder.Default
    String httpVersion = HttpRequestVersions.AUTO;
    Integer requestTimeoutMs;
}
