package com.laker.postman.http.runtime.redirect;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.RedirectInfo;


import com.laker.postman.http.runtime.transport.DefaultHttpTransport;
import com.laker.postman.http.runtime.transport.HttpExchangeOptions;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSink;
import com.laker.postman.http.runtime.sse.SseResponseCallback;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 负责处理重定向链
 */
@Slf4j
public class HttpRedirectExecutor {
    private final HttpTransport httpTransport;

    public HttpRedirectExecutor() {
        this(new DefaultHttpTransport());
    }

    public HttpRedirectExecutor(HttpTransport httpTransport) {
        this.httpTransport = httpTransport == null ? new DefaultHttpTransport() : httpTransport;
    }

    public HttpResponse executeWithRedirects(PreparedRequest req, int maxRedirects, SseResponseCallback callback) throws Exception {
        // 创建工作副本
        PreparedRequest workingReq = req.shallowCopy();

        // Collection 场景：启用完整的事件收集和日志输出
        workingReq.collectBasicInfo = true;  // 收集基本信息（headers、body）
        workingReq.collectEventInfo = true;  // 收集完整事件信息（DNS、连接等）
        workingReq.enableNetworkLog = true;  // 启用网络日志面板输出

        if (!workingReq.followRedirects || maxRedirects <= 0) {
            return executeAndSyncRequestMetadata(req, workingReq, callback);
        }

        // 重定向链由 HttpRedirectExecutor 统一处理，底层单次 OkHttp call 不能再自动跟随。
        // 否则 3xx 响应会被 OkHttp 吞掉，后续的重定向日志、跨域敏感头清理和最大跳转次数都会失效。
        workingReq.followRedirects = false;

        URL prevUrl = new URL(workingReq.url);
        int redirectCount = 0;

        while (true) {
            HttpResponse resp = executeAndSyncRequestMetadata(req, workingReq, callback);

            // 判断是否重定向
            RedirectInfo info = buildRedirectInfo(workingReq.url, resp);
            if (info.statusCode >= 300 && info.statusCode < 400 && info.location != null) {
                if (redirectCount >= maxRedirects) {
                    return resp;
                }

                URL nextUrl = info.location.startsWith("http") ? new URL(info.location) : new URL(prevUrl, info.location);
                boolean isCrossDomain = isCrossOrigin(prevUrl, nextUrl);

                redirectCount++;
                logRedirect(workingReq, info);

                // 基于当前请求创建下一次重定向请求
                workingReq = prepareRedirectRequest(workingReq, nextUrl.toString(), info.statusCode, isCrossDomain);
                prevUrl = nextUrl;
            } else {
                return resp;
            }
        }
    }

    private HttpResponse executeAndSyncRequestMetadata(PreparedRequest originalReq,
                                                       PreparedRequest workingReq,
                                                       SseResponseCallback callback) throws Exception {
        HttpResponse resp = httpTransport.execute(
                workingReq,
                HttpExchangeOptions.builder()
                        .callback(callback)
                        .build()
        );

        // 更新原始请求对象的 OkHttp 相关字段
        originalReq.sentHeadersList = workingReq.sentHeadersList;
        originalReq.sentRequestBody = workingReq.sentRequestBody;
        return resp;
    }

    /**
     * 构建重定向信息
     */
    private static RedirectInfo buildRedirectInfo(String url, HttpResponse resp) {
        RedirectInfo info = new RedirectInfo();
        info.url = url;
        info.statusCode = resp.code;
        info.headers = resp.headers;
        info.responseBody = resp.body;
        info.location = extractLocationHeader(resp);
        return info;
    }

    /**
     * 准备重定向请求
     */
    static PreparedRequest prepareRedirectRequest(PreparedRequest currentReq, String newUrl, int statusCode, boolean isCrossDomain) {
        PreparedRequest redirectReq = currentReq.shallowCopy();
        redirectReq.url = newUrl;
        boolean preserveRequestBody = statusCode == 307 || statusCode == 308;

        // 根据状态码处理 method 和 body
        if (!preserveRequestBody) {
            // 301/302/303 重定向：HEAD 保持不变，其余改为 GET，并清空 body
            if (!"HEAD".equalsIgnoreCase(redirectReq.method)) {
                redirectReq.method = "GET";
            }
            redirectReq.body = null;
            redirectReq.isMultipart = false;
            redirectReq.formDataList = null;
            redirectReq.urlencodedList = null;
        }
        // 307/308 保持原 method 和 body

        // 处理 headers：移除特定 header
        redirectReq.headersList = cleanHeadersList(
                redirectReq.headersList,
                isCrossDomain,
                preserveRequestBody,
                redirectReq.isMultipart
        );

        return redirectReq;
    }


    /**
     * 清理 List 结构的 headersList
     */
    static List<HttpHeader> cleanHeadersList(List<HttpHeader> headersList,
                                             boolean isCrossDomain,
                                             boolean preserveRequestBody,
                                             boolean isMultipartRequest) {
        if (headersList == null) {
            return Collections.emptyList();
        }

        List<HttpHeader> cleaned = new ArrayList<>(headersList);
        boolean shouldRemoveContentType = !preserveRequestBody || isMultipartRequest;
        cleaned.removeIf(h -> h.isEnabled() && (
                "Content-Length".equalsIgnoreCase(h.getKey()) ||
                        "Host".equalsIgnoreCase(h.getKey()) ||
                        (shouldRemoveContentType && "Content-Type".equalsIgnoreCase(h.getKey())) ||
                        (isCrossDomain && ("Authorization".equalsIgnoreCase(h.getKey()) || "Cookie".equalsIgnoreCase(h.getKey())))
        ));

        return cleaned;
    }

    static boolean isCrossOrigin(URL previousUrl, URL nextUrl) {
        if (previousUrl == null || nextUrl == null) {
            return false;
        }
        return !previousUrl.getProtocol().equalsIgnoreCase(nextUrl.getProtocol())
                || !previousUrl.getHost().equalsIgnoreCase(nextUrl.getHost())
                || effectivePort(previousUrl) != effectivePort(nextUrl);
    }

    private static int effectivePort(URL url) {
        int explicitPort = url.getPort();
        return explicitPort != -1 ? explicitPort : url.getDefaultPort();
    }

    /**
     * 记录重定向日志
     */
    private static void logRedirect(PreparedRequest request, RedirectInfo info) {
        if (request == null || !request.enableNetworkLog) {
            return;
        }
        try {
            String logMessage = String.format("Status: %d, URL: %s, Location: %s",
                    info.statusCode, info.url, info.location);
            NetworkLogSink sink = request.networkLogSink == null ? NetworkLogSink.noop() : request.networkLogSink;
            sink.append(NetworkLogEventStage.REDIRECT, logMessage, null);
        } catch (Exception e) {
            // Prevent logging errors from affecting the main redirect flow
        }
    }

    private static String extractLocationHeader(HttpResponse resp) {
        if (resp.headers != null) {
            for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                if (entry.getKey() != null && "Location".equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue().get(0);
                }
            }
        }
        return null;
    }
}
