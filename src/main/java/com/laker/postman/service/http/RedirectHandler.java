package com.laker.postman.service.http;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RedirectInfo;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.NetworkLogStage;
import com.laker.postman.service.http.sse.SseResEventListener;
import lombok.experimental.UtilityClass;
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
@UtilityClass
public class RedirectHandler {

    public static HttpResponse executeWithRedirects(PreparedRequest req, int maxRedirects, SseResEventListener callback) throws Exception {
        // 创建工作副本
        PreparedRequest workingReq = req.shallowCopy();
        workingReq.logEvent = true; // 记录事件日志

        URL prevUrl = new URL(workingReq.url);
        int redirectCount = 0;

        while (redirectCount <= maxRedirects) {
            HttpResponse resp = HttpSingleRequestExecutor.executeHttp(workingReq, callback);

            // 更新原始请求对象的 OkHttp 相关字段
            req.okHttpHeaders = workingReq.okHttpHeaders;
            req.okHttpRequestBody = workingReq.okHttpRequestBody;

            // 判断是否重定向
            RedirectInfo info = buildRedirectInfo(workingReq.url, resp);
            if (info.statusCode >= 300 && info.statusCode < 400 && info.location != null) {
                URL nextUrl = info.location.startsWith("http") ? new URL(info.location) : new URL(prevUrl, info.location);
                boolean isCrossDomain = !prevUrl.getHost().equalsIgnoreCase(nextUrl.getHost());

                redirectCount++;
                logRedirect(req.id, info);

                // 基于当前请求创建下一次重定向请求
                workingReq = prepareRedirectRequest(workingReq, nextUrl.toString(), info.statusCode, isCrossDomain);
                prevUrl = nextUrl;
            } else {
                return resp;
            }
        }
        return null;
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
    private static PreparedRequest prepareRedirectRequest(PreparedRequest currentReq, String newUrl, int statusCode, boolean isCrossDomain) {
        PreparedRequest redirectReq = currentReq.shallowCopy();
        redirectReq.url = newUrl;

        // 根据状态码处理 method 和 body
        if (statusCode == 301 || statusCode == 302 || statusCode == 303) {
            // 301/302/303 重定向：改为 GET，清空 body
            redirectReq.method = "GET";
            redirectReq.body = null;
            redirectReq.isMultipart = false;
            redirectReq.formDataList = null;
            redirectReq.urlencodedList = null;
        }
        // 307/308 保持原 method 和 body

        // 处理 headers：移除特定 header
        redirectReq.headersList = cleanHeadersList(redirectReq.headersList, isCrossDomain);

        return redirectReq;
    }


    /**
     * 清理 List 结构的 headersList
     */
    private static List<HttpHeader> cleanHeadersList(List<HttpHeader> headersList, boolean isCrossDomain) {
        if (headersList == null) {
            return Collections.emptyList();
        }

        List<HttpHeader> cleaned = new ArrayList<>(headersList);
        cleaned.removeIf(h -> h.isEnabled() && (
                "Content-Length".equalsIgnoreCase(h.getKey()) ||
                        "Host".equalsIgnoreCase(h.getKey()) ||
                        "Content-Type".equalsIgnoreCase(h.getKey()) ||
                        (isCrossDomain && ("Authorization".equalsIgnoreCase(h.getKey()) || "Cookie".equalsIgnoreCase(h.getKey())))
        ));

        return cleaned;
    }

    /**
     * 记录重定向日志
     */
    private static void logRedirect(String requestId, RedirectInfo info) {
        try {
            String logMessage = String.format("Status: %d, URL: %s, Location: %s",
                    info.statusCode, info.url, info.location);

            var editSubPanel = SingletonFactory.getInstance(RequestEditPanel.class)
                    .getRequestEditSubPanel(requestId);

            if (editSubPanel != null) {
                editSubPanel.getResponsePanel()
                        .getNetworkLogPanel()
                        .appendLog(NetworkLogStage.REDIRECT, logMessage);
            }
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