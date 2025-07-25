package com.laker.postman.service.render;

import com.laker.postman.model.*;
import com.laker.postman.service.http.HttpUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统一的HTTP请求/响应HTML渲染工具类
 * 用于生成所有HTTP相关的HTML展示内容
 */
public class HttpHtmlRenderer {

    // CSS样式常量
    private static final String BASE_STYLE = "font-family:monospace;";
    private static final String DETAIL_FONT_SIZE = "font-size:9px;";
    private static final String NORMAL_FONT_SIZE = "font-size:10px;";

    // 颜色常量
    private static final String COLOR_PRIMARY = "#1976d2";
    private static final String COLOR_SUCCESS = "#388e3c";
    private static final String COLOR_ERROR = "#d32f2f";
    private static final String COLOR_WARNING = "#ffa000";
    private static final String COLOR_GRAY = "#888";
    private static final String COLOR_BACKGROUND = "#f8f8f8";

    // 状态码颜色映射
    private static String getStatusColor(int code) {
        if (code >= 500) return COLOR_ERROR;
        if (code >= 400) return COLOR_WARNING;
        return "#43a047";
    }

    // HTML模板常量
    private static final String HTML_TEMPLATE = "<html><body style='%s'>%s</body></html>";
    private static final String DIVIDER = "<hr style='border:0;border-top:1.5px dashed #bbb;margin:12px 0;'>";
    private static final String TABLE_STYLE = "border='1' cellspacing='0' cellpadding='3'";
    private static final String PRE_STYLE = "";

    // 时间格式化器
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * 渲染完整的历史记录详情HTML
     */
    public static String renderHistoryDetail(RequestHistoryItem item) {
        if (item == null) {
            return createHtmlDocument(DETAIL_FONT_SIZE, "<div style='color:" + COLOR_GRAY + ";'>无历史记录详情</div>");
        }

        String content = buildBasicInfoHtml(item) +
                buildRequestHeadersHtml(item) +
                buildRequestBodyHtml(item) +
                DIVIDER +
                buildResponseHeadersHtml(item) +
                buildResponseBodyHtml(item) +
                DIVIDER +
                renderTimingInfo(item.response) +
                DIVIDER +
                renderEventInfo(item.response);

        return createHtmlDocument(DETAIL_FONT_SIZE, content);
    }

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
            return createHtmlDocument(NORMAL_FONT_SIZE, createNoDataDiv("无请求信息"));
        }

        String content = createLabelValue("URL", req.url) +
                createLabelValue("Method", req.method) +
                buildRequestHeadersTable(req) +
                buildRequestBodyContent(req);

        return createHtmlDocument(NORMAL_FONT_SIZE, content);
    }

    /**
     * 渲染响应信息HTML
     */
    public static String renderResponse(HttpResponse resp) {
        if (resp == null) {
            return createHtmlDocument(NORMAL_FONT_SIZE, "<span style='color:" + COLOR_GRAY + ";'>无响应信息</span>");
        }

        String content = createLabelValue("Status", String.valueOf(resp.code)) +
                createLabelValue("Protocol", safeString(resp.protocol)) +
                createLabelValue("Thread", safeString(resp.threadName)) +
                buildConnectionInfo(resp) +
                buildResponseHeadersTable(resp) +
                buildResponseBodyContent(resp);

        return createHtmlDocument(NORMAL_FONT_SIZE, content);
    }

    /**
     * 渲染测试结果HTML
     */
    public static String renderTestResults(List<TestResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return createHtmlDocument("font-size:9px;", "<div style='color:#888;padding:16px;'>No test results</div>");
        }
        StringBuilder table = new StringBuilder()
                .append("<table style='border-collapse:collapse;width:100%;background:#fff;'>")
                .append("<tr style='background:#f7f7f7;color:#222;font-weight:500;font-size:9px;'>")
                .append("<th style='padding:8px 12px;'>Name</th>")
                .append("<th style='padding:8px 12px;'>Result</th>")
                .append("<th style='padding:8px 12px;'>Error Message</th>")
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

    private static String createLabelValue(String label, String value) {
        return "<b>" + label + ":</b> " + escapeHtml(safeString(value)) + "<br/>";
    }

    private static String safeString(String str) {
        return str != null ? str : "-";
    }

    private static String buildBasicInfoHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder()
                .append("<div style='margin-bottom:12px;padding:6px 0;border-bottom:1px solid #e0e0e0;'>")
                .append("<span style='font-weight:bold;color:").append(COLOR_PRIMARY).append(";font-size:10px;'>")
                .append(item.method).append("</span> ")
                .append("<span style='color:").append(COLOR_SUCCESS).append(";font-size:10px;word-break:break-all;'>")
                .append(escapeHtml(item.url)).append("</span><br>")
                .append("<span style='color:").append(COLOR_GRAY).append(";'>Status</span>: ")
                .append("<span style='font-weight:bold;color:").append(getStatusColor(item.responseCode)).append(";'>")
                .append(item.responseCode).append("</span>  ");

        if (item.response != null) {
            sb.append(buildProtocolInfo(item.response))
                    .append(buildThreadInfo(item.response))
                    .append(buildConnectionDetails(item.response));
        }

        return sb.append("</div>").toString();
    }

    private static String buildProtocolInfo(HttpResponse response) {
        String protocol = response.protocol != null ? response.protocol : "-";
        return "<span style='color:" + COLOR_GRAY + ";'>Protocol</span>: " +
                "<span style='color:" + COLOR_PRIMARY + ";'>" + protocol + "</span>";
    }

    private static String buildThreadInfo(HttpResponse response) {
        String thread = response.threadName != null ? escapeHtml(response.threadName) : "-";
        return "<br><span style='color:" + COLOR_GRAY + ";'>Thread</span>: " +
                "<span style='color:" + COLOR_PRIMARY + ";'>" + thread + "</span>";
    }

    private static String buildConnectionDetails(HttpResponse response) {
        if (response.httpEventInfo != null) {
            String local = safeString(response.httpEventInfo.getLocalAddress());
            String remote = safeString(response.httpEventInfo.getRemoteAddress());
            return "<br><span style='color:" + COLOR_GRAY + ";'>Connection</span>: " +
                    "<span style='color:" + COLOR_PRIMARY + ";'>" + escapeHtml(local) + "</span> " +
                    "<span style='color:" + COLOR_GRAY + ";'>→</span> " +
                    "<span style='color:" + COLOR_PRIMARY + ";'>" + escapeHtml(remote) + "</span>";
        }
        return "";
    }

    private static String buildRequestHeadersHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder()
                .append("<div style='margin-bottom:8px;'><b style='color:").append(COLOR_PRIMARY).append(";'>[Request Headers]</b></div>")
                .append("<pre style='margin:0;'>");

        if (item.request != null && item.request.okHttpHeaders != null && item.request.okHttpHeaders.size() > 0) {
            for (int i = 0; i < item.request.okHttpHeaders.size(); i++) {
                String name = item.request.okHttpHeaders.name(i);
                String value = item.request.okHttpHeaders.value(i);
                sb.append(escapeHtml(name)).append(": ").append(escapeHtml(value)).append("\n");
            }
        } else {
            sb.append("(None)");
        }

        return sb.append("</pre>").toString();
    }

    private static String buildRequestBodyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder()
                .append("<div style='margin:8px 0 4px 0;'><b style='color:").append(COLOR_PRIMARY).append(";'>[Request Body]</b></div>");

        if (item.request == null) {
            return sb.append("<pre style='margin:0;'>(None)</pre>").toString();
        }

        boolean hasContent = false;

        // OkHttp请求体
        if (isNotEmpty(item.request.okHttpRequestBody)) {
            sb.append(buildBodySection(item.request.okHttpRequestBody));
            hasContent = true;
        }

        // Form数据
        hasContent |= appendFormData(sb, "form-data", item.request.formData);
        hasContent |= appendFormData(sb, "form-files", item.request.formFiles);
        hasContent |= appendFormData(sb, "x-www-form-urlencoded", item.request.urlencoded);

        if (!hasContent && isEmpty(item.request.body)) {
            sb.append("<pre style='margin:0;'>(None)</pre>");
        }

        return sb.toString();
    }

    private static boolean appendFormData(StringBuilder sb, String title, Map<String, String> data) {
        if (data != null && !data.isEmpty()) {
            sb.append("<b>").append(title).append("</b><br><pre style='margin:0;'>");
            data.forEach((key, value) ->
                    sb.append(escapeHtml(key)).append(" = ").append(escapeHtml(value)).append("\n"));
            sb.append("</pre>");
            return true;
        }
        return false;
    }

    private static String buildBodySection(String content) {
        return "<br><pre style='margin:0;" + PRE_STYLE + "'>" +
                escapeHtml(content) + "</pre>";
    }

    private static String buildResponseHeadersHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder()
                .append("<div style='margin-bottom:8px;'><b style='color:").append(COLOR_SUCCESS).append(";'>[Response Headers]</b></div>")
                .append("<pre style='margin:0;'>");

        if (item.response != null && item.response.headers != null && !item.response.headers.isEmpty()) {
            item.response.headers.forEach((key, values) -> {
                sb.append(escapeHtml(key)).append(": ");
                if (values != null && !values.isEmpty()) {
                    sb.append(escapeHtml(String.join(", ", values)));
                }
                sb.append("\n");
            });
        } else {
            sb.append("(None)");
        }

        sb.append("</pre>");

        if (item.response != null) {
            sb.append("<div style='color:").append(COLOR_GRAY).append(";font-size:10px;margin-bottom:4px;'>Headers Size: ")
                    .append(HttpUtil.getSizeText(item.response.headersSize)).append("</div>");
        }

        return sb.toString();
    }

    private static String buildResponseBodyHtml(RequestHistoryItem item) {
        StringBuilder sb = new StringBuilder()
                .append("<div style='margin:8px 0 4px 0;'><b style='color:").append(COLOR_SUCCESS).append(";'>[Response Body]</b></div>")
                .append("<pre style='margin:0;'>");

        if (item.response != null && item.response.body != null && !item.response.body.isEmpty()) {
            sb.append(escapeHtml(item.response.body));
        } else {
            sb.append("(None)");
        }

        sb.append("</pre>");

        if (item.response != null) {
            sb.append("<div style='color:").append(COLOR_GRAY).append(";font-size:10px;margin-bottom:4px;'>Body Size: ")
                    .append(HttpUtil.getSizeText(item.response.bodySize)).append("</div>");
        }

        return sb.toString();
    }

    private static String buildRequestHeadersTable(PreparedRequest req) {
        if (req.okHttpHeaders == null || req.okHttpHeaders.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder()
                .append("<b>请求头:</b><br/><table ").append(TABLE_STYLE).append(">");

        for (int i = 0; i < req.okHttpHeaders.size(); i++) {
            String name = req.okHttpHeaders.name(i);
            String value = req.okHttpHeaders.value(i);
            sb.append("<tr><td>").append(escapeHtml(name)).append(":</td><td>")
                    .append(escapeHtml(value)).append("</td></tr>");
        }

        return sb.append("</table>").toString();
    }

    private static String buildRequestBodyContent(PreparedRequest req) {
        StringBuilder sb = new StringBuilder();

        if (isNotEmpty(req.okHttpRequestBody)) {
            sb.append("<br/><pre style='").append(PRE_STYLE).append("'>")
                    .append(escapeHtml(req.okHttpRequestBody)).append("</pre>");
        }

        sb.append(buildFormDataTable("Form Data", req.formData))
                .append(buildFormDataTable("Form Files", req.formFiles))
                .append(buildFormDataTable("x-www-form-urlencoded", req.urlencoded));

        return sb.toString();
    }

    private static String buildFormDataTable(String title, Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder()
                .append("<b>").append(title).append(":</b><br/><table ").append(TABLE_STYLE).append(">");

        data.forEach((key, value) ->
                sb.append("<tr><td>").append(escapeHtml(key)).append(":</td><td>")
                        .append(escapeHtml(value)).append("</td></tr>"));

        return sb.append("</table>").toString();
    }

    private static String buildConnectionInfo(HttpResponse resp) {
        if (resp.httpEventInfo == null) {
            return "";
        }

        String local = safeString(resp.httpEventInfo.getLocalAddress());
        String remote = safeString(resp.httpEventInfo.getRemoteAddress());

        return "<b>Connection:</b> " + escapeHtml(local) +
                " <span style='color:" + COLOR_GRAY + ";'>→</span> " +
                escapeHtml(remote) + "<br/>";
    }

    private static String buildResponseHeadersTable(HttpResponse resp) {
        if (resp.headers == null || resp.headers.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder()
                .append("<b>响应头:</b><br/><table ").append(TABLE_STYLE).append(">");

        resp.headers.forEach((key, values) -> {
            sb.append("<tr><td>").append(escapeHtml(key)).append(":</td><td>");
            if (values != null && !values.isEmpty()) {
                sb.append(escapeHtml(String.join(", ", values)));
            }
            sb.append("</td></tr>");
        });

        return sb.append("</table>").toString();
    }

    private static String buildResponseBodyContent(HttpResponse resp) {
        String body = resp.body != null ? resp.body : "<无响应体>";
        return "<b>响应体:</b><br/><pre style='" + PRE_STYLE + "'>" +
                escapeHtml(body) + "</pre>";
    }

    private static String buildTestResultRow(TestResult testResult) {
        String rowBg = "background:#fff;";
        String icon = testResult.passed ? "<span style='color:#4CAF50;font-size:12px;'>&#10003;</span>" : "<span style='color:#F44336;font-size:12px;'>&#10007;</span>";
        String resultText = testResult.passed ? "<span style='color:#222;font-size:9px;'>Passed</span>" : "<span style='color:#F44336;font-size:9px;'>Failed</span>";
        String message = testResult.message != null && !testResult.message.isEmpty() ?
                "<div style='color:#F44336;font-size:9px;white-space:pre-wrap;word-break:break-all;'>" + escapeHtml(testResult.message) + "</div>"
                : "";

        return "<tr style='" + rowBg + "'>" +
                "<td style='padding:8px 12px;color:#222;font-size:9px;'>" + escapeHtml(testResult.name) + "</td>" +
                "<td style='padding:8px 12px;text-align:center;'>" + icon + "<br>" + resultText + "</td>" +
                "<td style='padding:8px 12px;'>" + message + "</td>" +
                "</tr>";
    }

    private static String buildTimingHtml(HttpResponse response) {
        HttpEventInfo info = response.httpEventInfo;
        TimingCalculator calc = new TimingCalculator(info);

        // 添加时序表格行

        return "<div style='font-size:9px;'><b style='color:" + COLOR_PRIMARY + ";'>[Timing]</b></div>" +
                "<div style='margin:8px 0;'>" +
                "<div style='font-size:9px;'><b style='color:" + COLOR_PRIMARY + ";'>[Timing Timeline]</b></div>" +
                "<table style='border-collapse:collapse;margin:8px 0;'>" +

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
                createTimingRowString("Connection Reused", calc.getConnectionReused(), null, false, "本次请求是否复用连接") +
                createTimingRowString("OkHttp Idle Connections", String.valueOf(response.idleConnectionCount), null, false, "OkHttp空闲连接数（快照）") +
                createTimingRowString("OkHttp Total Connections", String.valueOf(response.connectionCount), null, false, "OkHttp总连接数（快照）") +
                "</table>" +
                buildTimingDescription() +
                "</div>";
    }

    private static String createTimingRow(String name, long value, String color, boolean bold, String description) {
        return createTimingRowString(name, value >= 0 ? value + " " + "ms" : "-", color, bold, description);
    }

    private static String createTimingRowString(String name, String value, String color, boolean bold, String description) {
        String nameStyle = bold ? "font-weight:bold;" : "";
        String valueStyle = "";

        if (color != null) {
            nameStyle += "color:" + color + ";";
            valueStyle = "color:" + color + ";" + (bold ? "font-weight:bold;" : "");
        }

        return "<tr>" +
                "<td style='padding:2px 8px 2px 0;" + nameStyle + "'>" + (bold ? "<b>" + name + "</b>" : name) + "</td>" +
                "<td style='" + valueStyle + "'>" + value + "</td>" +
                "<td style='color:" + COLOR_GRAY + ";font-size:9px;'>" + description + "</td>" +
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
        // 背景色为 rgb(245,247,250)，并加大列间距
        return "<div style='font-size:9px;'><b style='color:" + COLOR_PRIMARY + ";'>[Event Info]</b></div>" +
                // 设置背景色和列间距
                "<table style='border-collapse:collapse;background:rgb(245,247,250);border-radius:4px;padding:6px 8px;color:#444;margin:8px 0;'>" +
                createEventRow("QueueStart", formatMillis(info.getQueueStart())) +
                createEventRow("Local", escapeHtml(info.getLocalAddress())) +
                createEventRow("Remote", escapeHtml(info.getRemoteAddress())) +
                createEventRow("Protocol", info.getProtocol() != null ? info.getProtocol().toString() : "-") +
                createEventRow("TLS", safeString(info.getTlsVersion())) +
                createEventRow("Thread", safeString(info.getThreadName())) +
                createEventRow("Error", info.getErrorMessage() != null ? escapeHtml(info.getErrorMessage()) : "-") +
                "<tr><td colspan='2'><hr style='border:0;border-top:1px dashed #bbb;margin:4px 0'></td></tr>" +
                createEventTimingRows(info) +
                "</table>";
    }

    private static String createEventRow(String label, String value) {
        // 增加 label 和 value 间距 120px
        return "<tr><td style='min-width:80px;padding:2px 120px 2px 0;color:" + COLOR_GRAY + ";'>" + label + "</td><td>" + value + "</td></tr>";
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
        return "<tr><td style='padding:2px 8px 2px 0;" + style + "'>" + label + "</td><td>" + formatMillis(millis) + "</td></tr>";
    }

    private static String formatMillis(long millis) {
        return millis <= 0 ? "-" : TIME_FORMATTER.format(new Date(millis));
    }

    private static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
            return info.getQueueingCost() > 0 ? info.getQueueingCost() : -1;
        }

        public long getStalled() {
            return info.getStalledCost() > 0 ? info.getStalledCost() : -1;
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
            return (reqHeaders >= 0 || reqBody >= 0) ?
                    (Math.max(reqHeaders, 0) + Math.max(reqBody, 0)) : -1;
        }

        public long getResponseBody() {
            return calculateDuration(info.getResponseBodyStart(), info.getResponseBodyEnd());
        }

        public long getServerCost() {
            if (info.getResponseHeadersStart() <= 0) return -1;

            long endTime = info.getRequestBodyEnd() > 0 ?
                    info.getRequestBodyEnd() : info.getRequestHeadersEnd();

            return endTime > 0 ? info.getResponseHeadersStart() - endTime : -1;
        }

        public String getConnectionReused() {
            if (info.getConnectionAcquired() <= 0) return "-";

            return (info.getConnectStart() == 0 || info.getConnectionAcquired() < info.getConnectStart()) ?
                    "Yes" : "No";
        }

        private long calculateDuration(long start, long end) {
            return (start > 0 && end > 0) ? end - start : -1;
        }
    }
}
