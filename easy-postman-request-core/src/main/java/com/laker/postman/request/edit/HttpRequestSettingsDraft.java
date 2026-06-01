package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpRequestVersions;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpRequestSettingsDraft {
    Boolean followRedirects;
    Boolean cookieJarEnabled;
    @Builder.Default
    String httpVersion = HttpRequestVersions.AUTO;
    Integer requestTimeoutMs;
}
