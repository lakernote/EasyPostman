package com.laker.postman.panel.history;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.service.http.HttpUtil;

public class HistoryHtmlBuilder {
    public static String formatHistoryDetailPrettyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:9px;'>");
        sb.append(buildBasicInfoHtml(item));
        sb.append(buildRequestHeadersHtml(item));
        sb.append(buildRequestBodyHtml(item));
        sb.append("<hr style='border:0;border-top:1.5px dashed #bbb;margin:12px 0;'>");
        sb.append(buildResponseHeadersHtml(item));
        sb.append(buildResponseBodyHtml(item));
        if (item.response != null && item.response.httpEventInfo != null) {
            sb.append(buildTimingHtml(item));
        }
        if (item.response != null && item.response.httpEventInfo != null) {
            sb.append(buildEventInfoHtml(item));
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    // 提供单独渲染Timing和Event Info的静态方法，供RunnerPanel复用
    public static String formatHistoryDetailPrettyHtml_Timing(RequestHistoryItem item) {
        if (item.response != null && item.response.httpEventInfo != null) {
            return buildTimingHtml(item);
        }
        return "<div style='color:#888;'>No Timing Info</div>";
    }

    public static String formatHistoryDetailPrettyHtml_EventInfo(RequestHistoryItem item) {
        if (item.response != null && item.response.httpEventInfo != null) {
            return buildEventInfoHtml(item);
        }
        return "<div style='color:#888;'>No Event Info</div>";
    }

    private static String buildBasicInfoHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-bottom:12px;padding:6px 0 6px 0;border-bottom:1px solid #e0e0e0;'>");
        sb.append("<span style='font-weight:bold;color:#1976d2;font-size:10px;'>")
                .append(item.method).append("</span> ");
        sb.append("<span style='color:#388e3c;font-size:10px;word-break:break-all;'>")
                .append(escapeHtml(item.url)).append("</span><br>");
        String codeColor = "#43a047";
        if (item.responseCode >= 500) codeColor = "#d32f2f";
        else if (item.responseCode >= 400) codeColor = "#ffa000";
        sb.append("<span style='color:#888;'>Status</span>: <span style='font-weight:bold;color:")
                .append(codeColor).append(";'>")
                .append(item.responseCode).append("</span>  ");
        sb.append("<span style='color:#888;'>Protocol</span>: <span style='color:#1976d2;'>")
                .append(item.response != null && item.response.protocol != null ? item.response.protocol : "-")
                .append("</span>");
        if (item.response != null) {
            sb.append("<br><span style='color:#888;'>Thread</span>: <span style='color:#1976d2;'>")
                    .append(item.response.threadName != null ? escapeHtml(item.response.threadName) : "-")
                    .append("</span>");
            if (item.response.httpEventInfo != null) {
                String local = item.response.httpEventInfo.getLocalAddress();
                String remote = item.response.httpEventInfo.getRemoteAddress();
                sb.append("<br><span style='color:#888;'>Connection</span>: <span style='color:#1976d2;'>")
                        .append(local != null ? escapeHtml(local) : "-")
                        .append("</span> <span style='color:#888;'>→</span> <span style='color:#1976d2;'>")
                        .append(remote != null ? escapeHtml(remote) : "-")
                        .append("</span>");
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String buildRequestHeadersHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>[Request Headers]</b></div>");
        sb.append("<pre style='margin:0;'>");
        if (item.request != null && item.request.okHttpHeaders != null && item.request.okHttpHeaders.size() > 0) {
            for (int i = 0; i < item.request.okHttpHeaders.size(); i++) {
                String name = item.request.okHttpHeaders.name(i);
                String value = item.request.okHttpHeaders.value(i);
                sb.append(escapeHtml(name)).append(": ").append(escapeHtml(value)).append("\n");
            }
        } else {
            sb.append("(None)");
        }
        sb.append("</pre>");
        return sb.toString();
    }

    private static String buildRequestBodyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin:8px 0 4px 0;'><b style='color:#1976d2;'>[Request Body]</b></div>");
        boolean hasForm = false;
        if (item.request != null) {
            // 优先展示真实OkHttp请求体内容（如有）
            if (item.request.okHttpRequestBody != null && !item.request.okHttpRequestBody.isEmpty()) {
                sb.append("<b>真实请求体</b><br><pre style='margin:0;background:#f8f8f8;border:1px solid #eee;padding:8px;'>")
                        .append(escapeHtml(item.request.okHttpRequestBody)).append("</pre>");
            }

            if (item.request.formData != null && !item.request.formData.isEmpty()) {
                hasForm = true;
                sb.append("<b>form-data</b><br><pre style='margin:0;'>");
                for (var entry : item.request.formData.entrySet()) {
                    sb.append(escapeHtml(entry.getKey())).append(" = ").append(escapeHtml(entry.getValue())).append("\n");
                }
                sb.append("</pre>");
            }
            if (item.request.formFiles != null && !item.request.formFiles.isEmpty()) {
                hasForm = true;
                sb.append("<b>form-files</b><br><pre style='margin:0;'>");
                for (var entry : item.request.formFiles.entrySet()) {
                    sb.append(escapeHtml(entry.getKey())).append(" = ").append(escapeHtml(entry.getValue())).append("\n");
                }
                sb.append("</pre>");
            }
            if (!hasForm && item.request.body != null && !item.request.body.isEmpty()) {
                sb.append("<b>request-body</b><br><pre style='margin:0;'>");
                sb.append("<pre style='margin:0;'>").append(escapeHtml(item.request.body)).append("</pre>");
            }
            if (!hasForm && (item.request.body == null || item.request.body.isEmpty()) && (item.request.okHttpRequestBody == null || item.request.okHttpRequestBody.isEmpty())) {
                sb.append("<pre style='margin:0;'>(None)</pre>");
            }
        } else {
            sb.append("<pre style='margin:0;'>(None)</pre>");
        }
        return sb.toString();
    }

    private static String buildResponseHeadersHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-bottom:8px;'><b style='color:#388e3c;'>[Response Headers]</b></div>");
        sb.append("<pre style='margin:0;'>");
        if (item.response != null && item.response.headers != null && !item.response.headers.isEmpty()) {
            for (var entry : item.response.headers.entrySet()) {
                sb.append(escapeHtml(entry.getKey())).append(": ");
                if (entry.getValue() != null) {
                    sb.append(escapeHtml(String.join(", ", entry.getValue())));
                }
                sb.append("\n");
            }
        } else {
            sb.append("(None)");
        }
        sb.append("</pre>");
        if (item.response != null) {
            sb.append("<div style='color:#888;font-size:10px;margin-bottom:4px;'>Headers Size: ")
                    .append(HttpUtil.getSizeText(item.response.headersSize)).append("</div>");
        }
        return sb.toString();
    }

    private static String buildResponseBodyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin:8px 0 4px 0;'><b style='color:#388e3c;'>[Response Body]</b></div>");
        sb.append("<pre style='margin:0;'>");
        if (item.response != null && item.response.body != null && !item.response.body.isEmpty()) {
            sb.append(escapeHtml(item.response.body));
        } else {
            sb.append("(None)");
        }
        sb.append("</pre>");
        if (item.response != null) {
            sb.append("<div style='color:#888;font-size:10px;margin-bottom:4px;'>Body Size: ")
                    .append(HttpUtil.getSizeText(item.response.bodySize)).append("</div>");
        }
        return sb.toString();
    }

    private static String buildTimingHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<hr style='border:0;border-top:2px solid #1976d2;margin:16px 0 8px 0;'>");
        sb.append("<div style='font-size:9px;'><b style='color:#1976d2;'>[Timing]</b></div>");
        HttpEventInfo info = item.response.httpEventInfo;
        long dns = info.getDnsEnd() > 0 && info.getDnsStart() > 0 ? info.getDnsEnd() - info.getDnsStart() : -1;
        long connect = info.getConnectEnd() > 0 && info.getConnectStart() > 0 ? info.getConnectEnd() - info.getConnectStart() : -1;
        long tls = info.getSecureConnectEnd() > 0 && info.getSecureConnectStart() > 0 ? info.getSecureConnectEnd() - info.getSecureConnectStart() : -1;
        long reqHeaders = info.getRequestHeadersEnd() > 0 && info.getRequestHeadersStart() > 0 ? info.getRequestHeadersEnd() - info.getRequestHeadersStart() : -1;
        long reqBody = info.getRequestBodyEnd() > 0 && info.getRequestBodyStart() > 0 ? info.getRequestBodyEnd() - info.getRequestBodyStart() : -1;
        long respBody = info.getResponseBodyEnd() > 0 && info.getResponseBodyStart() > 0 ? info.getResponseBodyEnd() - info.getResponseBodyStart() : -1;
        long total = info.getCallEnd() > 0 && info.getCallStart() > 0 ? info.getCallEnd() - info.getCallStart() : -1;
        long serverCost = -1;
        if (info.getResponseHeadersStart() > 0) {
            if (info.getRequestBodyEnd() > 0) {
                serverCost = info.getResponseHeadersStart() - info.getRequestBodyEnd();
            } else if (info.getRequestHeadersEnd() > 0) {
                serverCost = info.getResponseHeadersStart() - info.getRequestHeadersEnd();
            }
        }
        String reused = "-";
        if (info.getConnectionAcquired() > 0) {
            if (info.getConnectStart() == 0 || (info.getConnectionAcquired() < info.getConnectStart())) {
                reused = "Yes";
            } else {
                reused = "No";
            }
        }
        sb.append("<div style='margin:8px 0 8px 0;'>");
        sb.append("<div style='font-size:9px;'><b style='color:#1976d2;'>[Timing Timeline]</b></div>");
        sb.append("<table style='border-collapse:collapse;margin:8px 0 8px 0;'>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#333;'><b>Total</b></td><td style='color:#d32f2f;font-weight:bold;'>")
                .append(total >= 0 ? total + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>总耗时（CallStart→CallEnd，整个请求生命周期）</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>Queueing</td><td>")
                .append(info.getQueueingCost() > 0 ? info.getQueueingCost() + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>QueueStart→CallStart，排队等待调度</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>Stalled</td><td>")
                .append(info.getStalledCost() > 0 ? info.getStalledCost() + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>CallStart→ConnectStart，阻塞（包含DNS解析）</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>DNS Lookup</td><td>")
                .append(dns >= 0 ? dns + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>DnsStart→DnsEnd，域名解析（Stalled子阶段）</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>Initial Connection (TCP)</td><td>")
                .append(connect >= 0 ? connect + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>ConnectStart→ConnectEnd，TCP连接建立（包含SSL/TLS）</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>SSL/TLS</td><td>")
                .append(tls >= 0 ? tls + " ms" : "-")
                .append("</td><td style='color:#888;font-size:10px;'>SecureConnectStart→SecureConnectEnd，SSL/TLS握手（TCP连接子阶段）</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>Request Sent</td><td>")
                .append((reqHeaders >= 0 || reqBody >= 0) ? ((reqHeaders >= 0 ? reqHeaders : 0) + (reqBody >= 0 ? reqBody : 0)) + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>RequestHeadersStart/RequestBodyStart→RequestHeadersEnd/RequestBodyEnd，请求头和体发送</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'><b>Waiting (TTFB)</b></td><td style='color:#388e3c;font-weight:bold;'>")
                .append(serverCost >= 0 ? serverCost + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>RequestBodyEnd/RequestHeadersEnd→ResponseHeadersStart，服务端处理（TTFB���</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>Content Download</td><td>")
                .append(respBody >= 0 ? respBody + " ms" : "-")
                .append("</td><td style='color:#888;font-size:9px;'>ResponseBodyStart→ResponseBodyEnd，响应体下载</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>Connection Reused</td><td>")
                .append(reused)
                .append("</td><td style='color:#888;font-size:9px;'>本次请求是否复用连接</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>OkHttp Idle Connections</td><td>")
                .append(item.response.idleConnectionCount)
                .append("</td><td style='color:#888;font-size:9px;'>OkHttp空闲连接数（快照）</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>OkHttp Total Connections</td><td>")
                .append(item.response.connectionCount)
                .append("</td><td style='color:#888;font-size:9px;'>OkHttp总连接数（快照）</td></tr>");
        sb.append("</table>");
        sb.append("<div style='font-size:9px;color:#888;margin-top:2px;'>");
        sb.append("各阶段含义参考Chrome DevTools：Queueing(排队，OkHttp近似为newCall到callStart间)、Stalled(阻塞，近似为callStart到connectStart间)、DNS Lookup、Initial Connection (TCP)、SSL/TLS、Request Sent、Waiting (TTFB)(服务端处理)、Content Download(内容下载)。<br>");
        sb.append("Queueing和Stalled为近似值，受OkHttp实现限制，仅供参考。<br>");
        sb.append("Connection Reused=Yes 表示本次请求未新建TCP连接。<br>");
        sb.append("OkHttp Idle/Total Connections为请求时刻连接池快照，仅供参考。");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private static String buildEventInfoHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("<hr style='border:0;border-top:1.5px dashed #bbb;margin:12px 0'>");
        sb.append("<div style='font-size:9px;'><b style='color:#1976d2;'>[Event Info]</b></div>");
        HttpEventInfo info = item.response.httpEventInfo;
        sb.append("<table style='border-collapse:collapse;background:#f7f7f7;border-radius:4px;padding:6px 8px;color:#444;margin:8px 0 8px 0;'>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>QueueStart</td><td>" + formatMillis(info.getQueueStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Local</td><td>" + escapeHtml(info.getLocalAddress()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Remote</td><td>" + escapeHtml(info.getRemoteAddress()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Protocol</td><td>" + (info.getProtocol() != null ? info.getProtocol().toString() : "-") + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>TLS</td><td>" + (info.getTlsVersion() != null ? info.getTlsVersion() : "-") + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Thread</td><td>" + (info.getThreadName() != null ? info.getThreadName() : "-") + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#888;'>Error</td><td>" + (info.getErrorMessage() != null ? escapeHtml(info.getErrorMessage()) : "-") + "</td></tr>");
        sb.append("<tr><td colspan='2'><hr style='border:0;border-top:1px dashed #bbb;margin:4px 0'></td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>QueueStart</td><td>" + formatMillis(info.getQueueStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>CallStart</td><td>" + formatMillis(info.getCallStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>DnsStart</td><td>" + formatMillis(info.getDnsStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>DnsEnd</td><td>" + formatMillis(info.getDnsEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>ConnectStart</td><td>" + formatMillis(info.getConnectStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>SecureConnectStart</td><td>" + formatMillis(info.getSecureConnectStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>SecureConnectEnd</td><td>" + formatMillis(info.getSecureConnectEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>ConnectEnd</td><td>" + formatMillis(info.getConnectEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>ConnectionAcquired</td><td>" + formatMillis(info.getConnectionAcquired()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestHeadersStart</td><td>" + formatMillis(info.getRequestHeadersStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestHeadersEnd</td><td>" + formatMillis(info.getRequestHeadersEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestBodyStart</td><td>" + formatMillis(info.getRequestBodyStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>RequestBodyEnd</td><td>" + formatMillis(info.getRequestBodyEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseHeadersStart</td><td>" + formatMillis(info.getResponseHeadersStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseHeadersEnd</td><td>" + formatMillis(info.getResponseHeadersEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseBodyStart</td><td>" + formatMillis(info.getResponseBodyStart()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>ResponseBodyEnd</td><td>" + formatMillis(info.getResponseBodyEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0'>ConnectionReleased</td><td>" + formatMillis(info.getConnectionReleased()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#1976d2;'>CallEnd</td><td>" + formatMillis(info.getCallEnd()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#d32f2f;'>CallFailed</td><td>" + formatMillis(info.getCallFailed()) + "</td></tr>");
        sb.append("<tr><td style='padding:2px 8px 2px 0;color:#d32f2f;'>Canceled</td><td>" + formatMillis(info.getCanceled()) + "</td></tr>");
        sb.append("</table>");
        return sb.toString();
    }

    private static String formatMillis(long millis) {
        if (millis <= 0) return "-";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
        java.util.Date date = new java.util.Date(millis);
        return sdf.format(date);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
