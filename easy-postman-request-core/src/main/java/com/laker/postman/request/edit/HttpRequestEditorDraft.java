package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class HttpRequestEditorDraft {
    String id;
    String name;
    String description;
    String url;
    String method;
    RequestItemProtocolEnum protocol;
    @Builder.Default
    List<HttpHeader> headers = Collections.emptyList();
    @Builder.Default
    List<HttpParam> pathVariables = Collections.emptyList();
    @Builder.Default
    List<HttpParam> params = Collections.emptyList();
    String bodyType;
    String body;
    @Builder.Default
    List<HttpFormData> formData = Collections.emptyList();
    @Builder.Default
    List<HttpFormUrlencoded> urlencoded = Collections.emptyList();
    String authType;
    String authUsername;
    String authPassword;
    String authToken;
    Boolean followRedirects;
    Boolean cookieJarEnabled;
    @Builder.Default
    HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT;
    String httpVersion;
    Integer requestTimeoutMs;
    String prescript;
    String postscript;
    @Builder.Default
    List<SavedResponse> responses = Collections.emptyList();
}
