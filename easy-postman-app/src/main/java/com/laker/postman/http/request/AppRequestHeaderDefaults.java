package com.laker.postman.http.request;

import com.laker.postman.request.defaults.GeneratedRequestHeaderPolicy;
import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class AppRequestHeaderDefaults {

    public static GeneratedRequestHeaderPolicy generatedHeaderPolicy() {
        return GeneratedRequestHeaderPolicy.standard(HttpRequestFactory.EASY_POSTMAN_CLIENT);
    }

    public static List<HttpHeader> generatedHeaders() {
        return generatedHeaderPolicy().generatedHeaders();
    }
}
