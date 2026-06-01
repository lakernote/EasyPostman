package com.laker.postman.request.defaults;

import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class HttpRequestDefaults {
    public static final String USER_AGENT = "User-Agent";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String CONNECTION = "Connection";
    public static final String ACCEPT_ANY = "*/*";
    public static final String ACCEPT_ENCODING_VALUE = "gzip, deflate, br";
    public static final String CONNECTION_VALUE = "keep-alive";

    public static List<HttpHeader> standardHttpHeaders(String userAgentValue) {
        return List.of(
                new HttpHeader(true, USER_AGENT, userAgentValue),
                new HttpHeader(true, ACCEPT, ACCEPT_ANY),
                new HttpHeader(true, ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE),
                new HttpHeader(true, CONNECTION, CONNECTION_VALUE)
        );
    }
}
