package com.laker.postman.service.render;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.TestResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 统一的HTTP请求/响应HTML渲染工具类
 * 用于生成所有HTTP相关的HTML展示内容
 */
public class HttpHtmlRenderer {

    // 内容大小限制常量 - 防止内存溢出
    private static final int MAX_DISPLAY_SIZE = 20 * 1024;  // 20KB for safe display

    // CSS样式常量
    private static final String BASE_STYLE = "font-family:monospace;";
    private static final String DETAIL_FONT_SIZE = "font-size:9px;";

    // 颜色常量
    private static final String COLOR_PRIMARY = "#1976d2";
    private static final String COLOR_SUCCESS = "#388e3c";
    private static final String COLOR_ERROR = "#d32f2f";
    private static final String COLOR_WARNING = "#ffa000";
    private static final String COLOR_GRAY = "#888";

    // 状态码颜色映射
    private static String getStatusColor(int code) {
        if (code >= 500) return COLOR_ERROR;
        if (code >= 400) return COLOR_WARNING;
        return "#43a047";
    }

    // HTML模板常量
    private static final String HTML_TEMPLATE = "<html><body style='%s'>%s</body></html>";

    // 时间格式化器
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss.SSS");


    /**
     * 单独渲染Timing信息
     */
    public static String renderTimingInfo(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) {
            return createNoDataDiv("No Timing Info");
        }
        return buildTimingHtml(response);
    }

    /**
     * 单独渲染Event信息
     */
    public static String renderEventInfo(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) {
            return createNoDataDiv("No Event Info");
        }
        return buildEventInfoHtml(response.httpEventInfo);
    }

    /**
     * 渲染请求信息HTML
     */
    public static String renderRequest(PreparedRequest req) {
        if (req == null) {
            return createHtmlDocument(DETAIL_FONT_SIZE, "<div style='color:#888;padding:16px;'>无请求信息</div>");
        }
        StringBuilder sb = new StringBuilder();
        // 添加强制换行和宽度控制
        sb.append("<div style='background:rgb(245,247,250);border-radius:4px;padding:12px 16px;margin-bottom:12px;font-size:9px;width:100%;max-width:100%;overflow-wrap:break-word;word-break:break-all;box-sizing:border-box;'>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>URL</b>: <span style='color:#222;'>").append(escapeHtml(safeString(req.url))).append("</span></div>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>Method</b>: <span style='color:#222;'>").append(escapeHtml(safeString(req.method))).append("</span></div>");
        if (req.okHttpHeaders != null && req.okHttpHeaders.size() > 0) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Headers</b></div>");
            // 改用div块格式，不再使用表格
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            for (int i = 0; i < req.okHttpHeaders.size(); i++) {
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:rgb(245,247,250);border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(req.okHttpHeaders.name(i))).append(":</strong> ");
                sb.append("<span style='color:#222;'>").append(escapeHtml(req.okHttpHeaders.value(i))).append("</span>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
        if (req.formData != null && !req.formData.isEmpty()) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Form Data</b></div>");
            // 改用div块格式
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            req.formData.forEach((key, value) -> {
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:rgb(245,247,250);border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(key)).append(":</strong> ");
                sb.append("<span style='color:#222;'>").append(escapeHtml(value)).append("</span>");
                sb.append("</div>");
            });
            sb.append("</div>");
        }
        if (req.formFiles != null && !req.formFiles.isEmpty()) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Form Files</b></div>");
            // 改用div块格式
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            req.formFiles.forEach((key, value) -> {
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:rgb(245,247,250);border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(key)).append(":</strong> ");
                sb.append("<span style='color:#222;'>").append(escapeHtml(value)).append("</span>");
                sb.append("</div>");
            });
            sb.append("</div>");
        }
        if (req.urlencoded != null && !req.urlencoded.isEmpty()) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>x-www-form-urlencoded</b></div>");
            // 改用div块格式
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            req.urlencoded.forEach((key, value) -> {
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:rgb(245,247,250);border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(key)).append(":</strong> ");
                sb.append("<span style='color:#222;'>").append(escapeHtml(value)).append("</span>");
                sb.append("</div>");
            });
            sb.append("</div>");
        }
        if (isNotEmpty(req.okHttpRequestBody)) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Body</b></div>");
            String requestBody = safeTruncateContent(req.okHttpRequestBody);
            sb.append("<pre style='background:rgb(245,247,250);padding:8px;border-radius:4px;font-size:9px;color:#222;white-space:pre-wrap;word-break:break-all;overflow-wrap:break-word;width:100%;max-width:100%;margin:0;box-sizing:border-box;overflow:hidden;'>").append(escapeHtml(requestBody)).append("</pre>");
        }
        sb.append("</div>");
        return createHtmlDocument(DETAIL_FONT_SIZE, sb.toString());
    }

    /**
     * 渲染响应信息HTML
     */
    public static String renderResponse(HttpResponse resp) {
        if (resp == null) {
            return createHtmlDocument(DETAIL_FONT_SIZE, "<div style='color:#888;padding:16px;'>无响应信息</div>");
        }
        StringBuilder sb = new StringBuilder();
        // 使用更强制的宽度控制和换行策略
        sb.append("<div style='background:rgb(245,247,250);border-radius:4px;padding:12px 16px;margin-bottom:12px;font-size:9px;width:100%;max-width:100%;overflow-wrap:break-word;word-break:break-all;box-sizing:border-box;'>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#388e3c;'>Status</b>: <span style='color:").append(getStatusColor(resp.code)).append(";font-weight:bold;'>").append(escapeHtml(String.valueOf(resp.code))).append("</span></div>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>Protocol</b>: <span style='color:#222;'>").append(escapeHtml(safeString(resp.protocol))).append("</span></div>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>Thread</b>: <span style='color:#222;'>").append(escapeHtml(safeString(resp.threadName))).append("</span></div>");
        if (resp.httpEventInfo != null) {
            sb.append("<div style='margin-bottom:8px;word-break:break-all;overflow-wrap:break-word;'><b style='color:#1976d2;'>Connection</b>: <span style='color:#222;'>").append(escapeHtml(safeString(resp.httpEventInfo.getLocalAddress()))).append(" → ").append(escapeHtml(safeString(resp.httpEventInfo.getRemoteAddress()))).append("</span></div>");
        }
        if (resp.headers != null && !resp.headers.isEmpty()) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#388e3c;'>Headers</b></div>");
            // 简化表格设计，使用更直接的换行控制
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            resp.headers.forEach((key, values) -> {
                String valueStr = values != null ? String.join(", ", values) : "";
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:rgb(245,247,250);border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(key)).append(":</strong> ");
                sb.append("<span style='color:#222;'>").append(escapeHtml(valueStr)).append("</span>");
                sb.append("</div>");
            });
            sb.append("</div>");
        }
        sb.append("<div style='margin-bottom:8px;'><b style='color:#388e3c;'>Body</b></div>");
        String responseBody = safeTruncateContent(resp.body);
        // 使用更简单但更有效的换行控制
        sb.append("<pre style='background:rgb(245,247,250);padding:8px;border-radius:4px;font-size:9px;color:#222;white-space:pre-wrap;word-break:break-all;overflow-wrap:break-word;width:100%;max-width:100%;margin:0;box-sizing:border-box;overflow:hidden;'>").append(escapeHtml(responseBody)).append("</pre>");
        sb.append("</div>");
        return createHtmlDocument(DETAIL_FONT_SIZE, sb.toString());
    }

    /**
     * 渲染测试结果HTML
     */
    public static String renderTestResults(List<TestResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return createHtmlDocument("font-size:9px;", "<div style='color:#888;padding:16px;'>No test results</div>");
        }
        StringBuilder table = new StringBuilder()
                .append("<table style='border-collapse:collapse;width:100%;background:rgb(245,247,250);font-size:9px;border-radius:4px;'>")
                .append("<tr style='background:#f5f7fa;color:#222;font-weight:500;font-size:9px;border-bottom:1px solid #e0e0e0;'>")
                .append("<th style='padding:8px 12px;text-align:left;'>Name</th>")
                .append("<th style='padding:8px 12px;text-align:center;'>Result</th>") // 修改为居中对齐
                .append("<th style='padding:8px 12px;text-align:left;'>Error Message</th>")
                .append("</tr>");
        for (TestResult testResult : testResults) {
            if (testResult != null) {
                table.append(buildTestResultRow(testResult));
            }
        }
        table.append("</table>");
        return createHtmlDocument("font-size:9px;", table.toString());
    }

    // ==================== 私有辅助方法 ====================

    private static String createHtmlDocument(String fontSize, String content) {
        return String.format(HTML_TEMPLATE, BASE_STYLE + fontSize, content);
    }

    private static String createNoDataDiv(String message) {
        return "<div style='color:" + COLOR_GRAY + ";'>" + message + "</div>";
    }

    private static String safeString(String str) {
        return str != null ? str : "-";
    }


    private static String buildTestResultRow(TestResult testResult) {
        String rowBg = "background:rgb(245,247,250);";
        String icon = testResult.passed ? "<span style='color:#4CAF50;font-size:9px;'>&#10003;</span>" : "<span style='color:#F44336;font-size:9px;'>&#10007;</span>";
        String message = testResult.message != null && !testResult.message.isEmpty() ?
                "<div style='color:#F44336;font-size:9px;white-space:pre-wrap;word-break:break-all;'>" + escapeHtml(testResult.message) + "</div>"
                : "";

        return "<tr style='" + rowBg + "border-bottom:1px solid #e0e0e0;'>" +
                "<td style='padding:8px 12px;color:#222;font-size:9px;'>" + escapeHtml(testResult.name) + "</td>" +
                "<td style='padding:8px 12px;text-align:center;vertical-align:middle;'>" + icon + "</td>" + // 增加 vertical-align:middle
                "<td style='padding:8px 12px;'>" + message + "</td>" +
                "</tr>";
    }

    private static String buildTimingHtml(HttpResponse response) {
        HttpEventInfo info = response.httpEventInfo;
        TimingCalculator calc = new TimingCalculator(info);

        // 改回表格格式，但添加强制换行控制
        return "<div style='width:100%;max-width:100%;overflow-wrap:break-word;box-sizing:border-box;'>" +
                "<div style='font-size:9px;margin-bottom:8px;'><b style='color:" + COLOR_PRIMARY + ";'>[Timeline]</b></div>" +
                "<table style='border-collapse:collapse;margin:4px 0;width:100%;table-layout:fixed;'>" +

                // 添加时序表格行
                createTimingRow("Total", calc.getTotal(), COLOR_ERROR, true, "总耗时（CallStart→CallEnd，整个请求生命周期）") +
                createTimingRow("Queueing", calc.getQueueing(), null, false, "QueueStart→CallStart，排队等待调度") +
                createTimingRow("Stalled", calc.getStalled(), null, false, "CallStart→ConnectStart，阻塞（包含DNS解析）") +
                createTimingRow("DNS Lookup", calc.getDns(), null, false, "DnsStart→DnsEnd，域名解析（Stalled子阶段）") +
                createTimingRow("Initial Connection (TCP)", calc.getConnect(), null, false, "ConnectStart→ConnectEnd，TCP连接建立（包含SSL/TLS）") +
                createTimingRow("SSL/TLS", calc.getTls(), null, false, "SecureConnectStart→SecureConnectEnd，SSL/TLS握手（TCP连接子阶段）") +
                createTimingRow("Request Sent", calc.getRequestSent(), null, false, "RequestHeadersStart/RequestBodyStart→RequestHeadersEnd/RequestBodyEnd，请求头和体发送") +
                createTimingRow("Waiting (TTFB)", calc.getServerCost(), COLOR_SUCCESS, true, "RequestBodyEnd/RequestHeadersEnd→ResponseHeadersStart，服务端处理") +
                createTimingRow("Content Download", calc.getResponseBody(), null, false, "ResponseBodyStart→ResponseBodyEnd，响应体下载") +
                createTimingRowString("Connection Reused", calc.getConnectionReused() ? "Yes" : "No", null, false, "本次请求是否复用连接") +
                createTimingRowString("OkHttp Idle Connections", String.valueOf(response.idleConnectionCount), null, false, "OkHttp空闲连接数（快照）") +
                createTimingRowString("OkHttp Total Connections", String.valueOf(response.connectionCount), null, false, "OkHttp总连接数（快照）") +
                "</table>" +
                buildTimingDescription() +
                "</div>";
    }

    private static String createTimingRow(String name, long value, String color, boolean bold, String description) {
        return createTimingRowString(name, value >= 0 ? value + " ms" : "-", color, bold, description);
    }

    private static String createTimingRowString(String name, String value, String color, boolean bold, String description) {
        String nameStyle = bold ? "font-weight:bold;" : "";
        String valueStyle = "";

        if (color != null) {
            nameStyle += "color:" + color + ";";
            valueStyle = "color:" + color + ";" + (bold ? "font-weight:bold;" : "");
        }

        return "<tr>" +
                "<td style='padding:2px 8px 2px 0;" + nameStyle + "width:25%;word-wrap:break-word;overflow-wrap:break-word;'>" + (bold ? "<b>" + name + "</b>" : name) + "</td>" +
                "<td style='" + valueStyle + "width:15%;word-wrap:break-word;overflow-wrap:break-word;'>" + value + "</td>" +
                "<td style='color:" + COLOR_GRAY + ";font-size:9px;width:60%;word-wrap:break-word;overflow-wrap:break-word;'>" + description + "</td>" +
                "</tr>";
    }

    private static String buildTimingDescription() {
        return "<div style='font-size:9px;color:" + COLOR_GRAY + ";margin-top:2px;'>" +
                "各阶段含义参考Chrome DevTools：Queueing(排队，OkHttp近似为newCall到callStart间)、Stalled(阻塞，近似为callStart到connectStart间)、" +
                "DNS Lookup、Initial Connection (TCP)、SSL/TLS、Request Sent、Waiting (TTFB)(服务端处理)、Content Download(内容下载)。<br>" +
                "Queueing和Stalled为近似值，受OkHttp实现限制，仅供参考。<br>" +
                "Connection Reused=Yes 表示本次请求未新建TCP连接。<br>" +
                "OkHttp Idle/Total Connections为请求时刻连接池快照，仅供参考。" +
                "</div>";
    }

    private static String buildEventInfoHtml(HttpEventInfo info) {
        // 改回表格格式，但添加强制换行控制
        return "<div style='width:100%;max-width:100%;overflow-wrap:break-word;box-sizing:border-box;'>" +
                "<div style='font-size:9px;margin-bottom:8px;'><b style='color:" + COLOR_PRIMARY + ";'>[Event Info]</b></div>" +
                // 设置背景色和列间距，添加强制换行
                "<table style='border-collapse:collapse;background:rgb(245,247,250);border-radius:4px;padding:3px 4px;color:#444;margin:4px 0;width:100%;table-layout:fixed;'>" +
                createEventRow("QueueStart", formatMillis(info.getQueueStart())) +
                createEventRow("Local", escapeHtml(info.getLocalAddress())) +
                createEventRow("Remote", escapeHtml(info.getRemoteAddress())) +
                createEventRow("Protocol", info.getProtocol() != null ? info.getProtocol().toString() : "-") +
                createEventRow("TLS", safeString(info.getTlsVersion())) +
                createEventRow("Thread", safeString(info.getThreadName())) +
                createEventRow("Error", info.getErrorMessage() != null ? escapeHtml(info.getErrorMessage()) : "-") +
                "<tr><td colspan='2'><hr style='border:0;border-top:1px dashed #bbb;margin:4px 0'></td></tr>" +
                createEventTimingRows(info) +
                "</table>" +
                "</div>";
    }

    private static String createEventRow(String label, String value) {
        // 增加换行控制和固定列宽
        return "<tr><td style='min-width:80px;padding:2px 120px 2px 0;color:" + COLOR_GRAY + ";width:30%;word-wrap:break-word;overflow-wrap:break-word;'>" + label + "</td><td style='width:70%;word-wrap:break-word;overflow-wrap:break-word;'>" + value + "</td></tr>";
    }

    private static String createEventTimingRows(HttpEventInfo info) {

        // 重要的时间点用特殊颜色标记

        return createEventTimingRow("QueueStart", info.getQueueStart(), COLOR_PRIMARY) +
                createEventTimingRow("CallStart", info.getCallStart(), COLOR_PRIMARY) +
                createEventTimingRow("DnsStart", info.getDnsStart(), COLOR_PRIMARY) +
                createEventTimingRow("DnsEnd", info.getDnsEnd(), COLOR_PRIMARY) +
                createEventTimingRow("ConnectStart", info.getConnectStart(), COLOR_PRIMARY) +
                createEventTimingRow("SecureConnectStart", info.getSecureConnectStart(), COLOR_PRIMARY) +
                createEventTimingRow("SecureConnectEnd", info.getSecureConnectEnd(), COLOR_PRIMARY) +
                createEventTimingRow("ConnectEnd", info.getConnectEnd(), COLOR_PRIMARY) +
                createEventTimingRow("ConnectionAcquired", info.getConnectionAcquired(), COLOR_PRIMARY) +
                createEventTimingRow("RequestHeadersStart", info.getRequestHeadersStart(), null) +
                createEventTimingRow("RequestHeadersEnd", info.getRequestHeadersEnd(), null) +
                createEventTimingRow("RequestBodyStart", info.getRequestBodyStart(), null) +
                createEventTimingRow("RequestBodyEnd", info.getRequestBodyEnd(), null) +
                createEventTimingRow("ResponseHeadersStart", info.getResponseHeadersStart(), null) +
                createEventTimingRow("ResponseHeadersEnd", info.getResponseHeadersEnd(), null) +
                createEventTimingRow("ResponseBodyStart", info.getResponseBodyStart(), null) +
                createEventTimingRow("ResponseBodyEnd", info.getResponseBodyEnd(), null) +
                createEventTimingRow("ConnectionReleased", info.getConnectionReleased(), null) +
                createEventTimingRow("CallEnd", info.getCallEnd(), COLOR_PRIMARY) +
                createEventTimingRow("CallFailed", info.getCallFailed(), COLOR_ERROR) +
                createEventTimingRow("Canceled", info.getCanceled(), COLOR_ERROR);
    }

    private static String createEventTimingRow(String label, long millis, String color) {
        String style = color != null ? "color:" + color + ";" : "";
        return "<tr><td style='padding:2px 8px 2px 0;" + style + "width:30%;word-wrap:break-word;overflow-wrap:break-word;'>" + label + "</td><td style='width:70%;word-wrap:break-word;overflow-wrap:break-word;'>" + formatMillis(millis) + "</td></tr>";
    }

    private static String formatMillis(long millis) {
        return millis <= 0 ? "-" : TIME_FORMATTER.format(new Date(millis));
    }

    private static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Safely truncate content to prevent memory overflow
     */
    private static String safeTruncateContent(String content) {
        if (content == null) {
            return "";
        }

        if (content.length() > MAX_DISPLAY_SIZE) {
            String truncated = content.substring(0, MAX_DISPLAY_SIZE);
            return truncated + "......\n\n[Content too large, truncated for display. Original length: " + content.length() + " characters, max displayed: " + (MAX_DISPLAY_SIZE / 1024) + "KB]";
        }

        return content;
    }

    // 时序计算辅助类
    private static class TimingCalculator {
        private final HttpEventInfo info;

        public TimingCalculator(HttpEventInfo info) {
            this.info = info;
        }

        public long getTotal() {
            return calculateDuration(info.getCallStart(), info.getCallEnd());
        }

        public long getQueueing() {
            return info.getQueueingCost() > 0 ? info.getQueueingCost() :
                    calculateDuration(info.getQueueStart(), info.getCallStart());
        }

        public long getStalled() {
            return info.getStalledCost() > 0 ? info.getStalledCost() :
                    calculateDuration(info.getCallStart(), info.getConnectStart());
        }

        public long getDns() {
            return calculateDuration(info.getDnsStart(), info.getDnsEnd());
        }

        public long getConnect() {
            return calculateDuration(info.getConnectStart(), info.getConnectEnd());
        }

        public long getTls() {
            return calculateDuration(info.getSecureConnectStart(), info.getSecureConnectEnd());
        }

        public long getRequestSent() {
            long reqHeaders = calculateDuration(info.getRequestHeadersStart(), info.getRequestHeadersEnd());
            long reqBody = calculateDuration(info.getRequestBodyStart(), info.getRequestBodyEnd());

            if (reqHeaders >= 0 && reqBody >= 0) {
                return reqHeaders + reqBody;
            } else if (reqHeaders >= 0) {
                return reqHeaders;
            } else if (reqBody >= 0) {
                return reqBody;
            }
            return -1;
        }

        public long getServerCost() {
            if (info.getResponseHeadersStart() <= 0) {
                return -1;
            }

            // 优先使用 RequestBodyEnd，如果没有则使用 RequestHeadersEnd
            long requestEndTime = info.getRequestBodyEnd() > 0 ?
                    info.getRequestBodyEnd() : info.getRequestHeadersEnd();

            if (requestEndTime <= 0) {
                return -1;
            }

            return calculateDuration(requestEndTime, info.getResponseHeadersStart());
        }

        public long getResponseBody() {
            return calculateDuration(info.getResponseBodyStart(), info.getResponseBodyEnd());
        }

        public boolean getConnectionReused() {
            // 如果没有连接获取事件，无法判断
            if (info.getConnectionAcquired() <= 0) {
                return false;
            }

            // 如果连接获取时间早于连接开始时间，说明是复用的连接
            return info.getConnectStart() <= 0 || info.getConnectionAcquired() < info.getConnectStart();
        }

        private long calculateDuration(long start, long end) {
            if (start <= 0 || end <= 0 || end < start) {
                return -1;
            }

            long duration = end - start;

            // 防止异常大的时间差（超过1小时认为异常）
            if (duration > 3600000) { // 3600000ms = 1小时
                return -1;
            }

            return duration;
        }
    }
}
