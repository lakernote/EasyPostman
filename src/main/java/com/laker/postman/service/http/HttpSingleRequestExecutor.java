package com.laker.postman.service.http;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class HttpSingleRequestExecutor {

    // Cookie 管理器
    private static final CookieManager COOKIE_MANAGER = new CookieManager();

    public static void registerCookieChangeListener(Runnable listener) {
        COOKIE_MANAGER.registerListener(listener);
    }

    public static void unregisterCookieChangeListener(Runnable listener) {
        COOKIE_MANAGER.unregisterListener(listener);
    }

    public static String getCookieHeader(String host) {
        return COOKIE_MANAGER.getCookieHeader(host);
    }

    public static void setCookies(String host, List<String> setCookieHeaders) {
        COOKIE_MANAGER.setCookies(host, setCookieHeaders);
    }

    public static Map<String, Map<String, String>> getAllCookies() {
        return COOKIE_MANAGER.getAllCookies();
    }

    public static HttpResponse execute(PreparedRequest req) throws Exception {
        HttpResponse resp = sendRequestByType(req);
        handleSetCookie(resp, req.url);
        return resp;
    }

    static HttpResponse sendRequestByType(PreparedRequest req) throws Exception {
        if (req.isMultipart) {
            return HttpService.sendRequestWithMultipart(req.url, req.method, req.headers, req.formData, req.formFiles, req.followRedirects);
        } else {
            return HttpService.sendRequest(req.url, req.method, req.headers, req.body, req.followRedirects);
        }
    }

    static void handleSetCookie(HttpResponse resp, String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost();
            List<String> setCookieHeaders = HttpRequestUtil.extractSetCookieHeaders(resp);
            if (!setCookieHeaders.isEmpty()) {
                setCookies(host, setCookieHeaders);
            }
        } catch (Exception ignore) {
        }
    }
}