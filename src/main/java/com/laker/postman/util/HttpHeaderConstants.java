package com.laker.postman.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HTTP 请求头常量和工具类
 */
@UtilityClass
public class HttpHeaderConstants {

    /**
     * 常见的HTTP请求头列表（按字母顺序排列）
     * 参考 Postman 和标准 HTTP 规范
     */
    public static final List<String> COMMON_HEADERS = Collections.unmodifiableList(Arrays.asList(
            // A
            "Accept",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Accept-Datetime",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Authorization",
            // C
            "Cache-Control",
            "Connection",
            "Content-Encoding",
            "Content-Length",
            "Content-MD5",
            "Content-Type",
            "Cookie",
            // D
            "Date",
            "DNT",
            // E
            "Expect",
            // F
            "Forwarded",
            "From",
            // H
            "Host",
            "HTTP2-Settings",
            // I
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Range",
            "If-Unmodified-Since",
            // M
            "Max-Forwards",
            // O
            "Origin",
            // P
            "Pragma",
            "Proxy-Authorization",
            // R
            "Range",
            "Referer",
            // T
            "TE",
            "Trailer",
            "Transfer-Encoding",
            // U
            "User-Agent",
            "Upgrade",
            "Upgrade-Insecure-Requests",
            // V
            "Via",
            // W
            "Warning",
            // X (常见的自定义头)
            "X-Requested-With",
            "X-Forwarded-For",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-CSRF-Token",
            "X-Request-ID",
            "X-Correlation-ID",
            "X-API-Key",
            "X-Auth-Token"
    ));

    /**
     * 常见的 Content-Type 值
     */
    public static final List<String> COMMON_CONTENT_TYPES = Collections.unmodifiableList(Arrays.asList(
            "application/json",
            "application/xml",
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "text/plain",
            "text/html",
            "text/xml",
            "application/javascript",
            "application/pdf",
            "application/octet-stream",
            "image/jpeg",
            "image/png",
            "image/gif"
    ));

    /**
     * 常见的 Accept 值
     */
    public static final List<String> COMMON_ACCEPT_VALUES = Collections.unmodifiableList(Arrays.asList(
            "*/*",
            "application/json",
            "application/xml",
            "text/html",
            "text/plain",
            "application/json, text/plain, */*"
    ));

    /**
     * 常见的 Accept-Encoding 值
     */
    public static final List<String> COMMON_ACCEPT_ENCODING_VALUES = Collections.unmodifiableList(Arrays.asList(
            "gzip, deflate, br",
            "gzip, deflate",
            "gzip",
            "deflate",
            "br",
            "identity"
    ));

    /**
     * 常见的 Cache-Control 值
     */
    public static final List<String> COMMON_CACHE_CONTROL_VALUES = Collections.unmodifiableList(Arrays.asList(
            "no-cache",
            "no-store",
            "max-age=0",
            "max-age=3600",
            "must-revalidate",
            "public",
            "private"
    ));

    /**
     * 常见的 Connection 值
     */
    public static final List<String> COMMON_CONNECTION_VALUES = Collections.unmodifiableList(Arrays.asList(
            "keep-alive",
            "close",
            "upgrade"
    ));

    /**
     * 根据请求头名称获取对应的常见值列表
     */
    public static List<String> getCommonValuesForHeader(String headerName) {
        if (headerName == null || headerName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedHeader = headerName.trim();

        switch (normalizedHeader) {
            case "Content-Type":
                return COMMON_CONTENT_TYPES;
            case "Accept":
                return COMMON_ACCEPT_VALUES;
            case "Accept-Encoding":
                return COMMON_ACCEPT_ENCODING_VALUES;
            case "Cache-Control":
                return COMMON_CACHE_CONTROL_VALUES;
            case "Connection":
                return COMMON_CONNECTION_VALUES;
            default:
                return Collections.emptyList();
        }
    }
}
