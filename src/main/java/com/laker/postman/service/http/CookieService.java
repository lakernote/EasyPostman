package com.laker.postman.service.http;

import java.util.List;
import java.util.Map;

/**
 * Cookie 管理相关工具类，负责监听、获取、设置 Cookie
 */
public class CookieService {
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

    public static void handleSetCookie(String url, List<String> setCookieHeaders) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost();
            if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                setCookies(host, setCookieHeaders);
            }
        } catch (Exception ignore) {
        }
    }
}

