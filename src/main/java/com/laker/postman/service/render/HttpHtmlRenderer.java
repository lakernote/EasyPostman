package com.laker.postman.service.render;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.model.*;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 统一的HTTP请求/响应HTML渲染工具类
 * 用于生成所有HTTP相关的HTML展示内容
 */
@UtilityClass
public class HttpHtmlRenderer {

    // 内容大小限制常量 - 防止内存溢出
    private static final int MAX_DISPLAY_SIZE = 2 * 1024;  // 2KB for safe display

    // CSS样式常量
    private static final String BASE_STYLE = "font-family:monospace;";
    private static final String DETAIL_FONT_SIZE = "font-size:9px;";

    // 颜色常量
    private static final String COLOR_PRIMARY = "#1976d2";
    private static final String COLOR_SUCCESS = "#388e3c";
    private static final String COLOR_ERROR = "#d32f2f";
    private static final String COLOR_WARNING = "#ffa000";
    private static final String COLOR_GRAY = "#888";

    /**
     * 检查当前是否为暗色主题
     */
    private static boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的背景色
     * 面板背景色（暗色主题）RGB(60, 63, 65) = #3c3f41
     * 面板背景色（亮色主题）RGB(242, 242, 242) = #f2f2f2
     */
    private static String getThemeBackground() {
        return isDarkTheme() ? "rgb(60, 63, 65)" : "rgb(242, 242, 242)";
    }

    /**
     * 获取主题适配的文字颜色
     */
    private static String getThemeTextColor() {
        return isDarkTheme() ? "#e0e0e0" : "#222";
    }

    /**
     * 获取主题适配的边框颜色
     */
    private static String getThemeBorderColor() {
        return isDarkTheme() ? "#4a4a4a" : "#e0e0e0";
    }

    /**
     * 获取主题适配的表头背景色
     */
    private static String getThemeHeaderBackground() {
        return isDarkTheme() ? "rgb(50, 52, 54)" : "#f5f7fa";
    }

    // 状态码颜色映射
    private static String getStatusColor(int code) {
        if (code >= 500) return COLOR_ERROR;
        if (code >= 400) return COLOR_WARNING;
        return "#43a047";
    }

    // HTML模板常量
    private static final String HTML_TEMPLATE = "<html><body style='%s'>%s</body></html>";


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

        String bgColor = getThemeBackground();
        String textColor = getThemeTextColor();

        StringBuilder sb = new StringBuilder();
        // 添加强制换行和宽度控制
        sb.append("<div style='background:").append(bgColor).append(";border-radius:4px;padding:12px 16px;margin-bottom:12px;font-size:9px;width:100%;max-width:100%;overflow-wrap:break-word;word-break:break-all;box-sizing:border-box;'>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>URL</b>: <span style='color:").append(textColor).append(";'>").append(escapeHtml(safeString(req.url))).append("</span></div>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>Method</b>: <span style='color:").append(textColor).append(";'>").append(escapeHtml(safeString(req.method))).append("</span></div>");
        if (req.okHttpHeaders != null && req.okHttpHeaders.size() > 0) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Headers</b></div>");
            // 改用div块格式，不再使用表格
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            for (int i = 0; i < req.okHttpHeaders.size(); i++) {
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:").append(bgColor).append(";border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(req.okHttpHeaders.name(i))).append(":</strong> ");
                sb.append("<span style='color:").append(textColor).append(";'>").append(escapeHtml(req.okHttpHeaders.value(i))).append("</span>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }
        if (req.formDataList != null && !req.formDataList.isEmpty()) {
            boolean hasText = req.formDataList.stream().anyMatch(d -> d.isEnabled() && d.isText());
            boolean hasFile = req.formDataList.stream().anyMatch(d -> d.isEnabled() && d.isFile());

            if (hasText) {
                sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Form Data</b></div>");
                sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
                req.formDataList.stream()
                        .filter(d -> d.isEnabled() && d.isText())
                        .forEach(data -> {
                            sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:").append(bgColor).append(";border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                            sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(data.getKey())).append(":</strong> ");
                            sb.append("<span style='color:").append(textColor).append(";'>").append(escapeHtml(data.getValue())).append("</span>");
                            sb.append("</div>");
                        });
                sb.append("</div>");
            }

            if (hasFile) {
                sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Form Files</b></div>");
                sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
                req.formDataList.stream()
                        .filter(d -> d.isEnabled() && d.isFile())
                        .forEach(data -> {
                            sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:").append(bgColor).append(";border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                            sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(data.getKey())).append(":</strong> ");
                            sb.append("<span style='color:").append(textColor).append(";'>").append(escapeHtml(data.getValue())).append("</span>");
                            sb.append("</div>");
                        });
                sb.append("</div>");
            }
        }
        if (req.urlencodedList != null && !req.urlencodedList.isEmpty()) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>x-www-form-urlencoded</b></div>");
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            req.urlencodedList.stream()
                    .filter(HttpFormUrlencoded::isEnabled)
                    .forEach(encoded -> {
                        sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:").append(bgColor).append(";border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                        sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(encoded.getKey())).append(":</strong> ");
                        sb.append("<span style='color:").append(textColor).append(";'>").append(escapeHtml(encoded.getValue())).append("</span>");
                        sb.append("</div>");
                    });
            sb.append("</div>");
        }
        if (isNotEmpty(req.okHttpRequestBody)) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#1976d2;'>Body</b></div>");
            String requestBody = safeTruncateContent(req.okHttpRequestBody);
            sb.append("<pre style='background:").append(bgColor).append(";padding:8px;border-radius:4px;font-size:9px;color:").append(textColor).append(";white-space:pre-wrap;word-break:break-all;overflow-wrap:break-word;width:100%;max-width:100%;margin:0;box-sizing:border-box;overflow:hidden;'>").append(escapeHtml(requestBody)).append("</pre>");
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

        String bgColor = getThemeBackground();
        String textColor = getThemeTextColor();

        StringBuilder sb = new StringBuilder();
        // 使用更强制的宽度控制和换行策略
        sb.append("<div style='background:").append(bgColor).append(";border-radius:4px;padding:12px 16px;margin-bottom:12px;font-size:9px;width:100%;max-width:100%;overflow-wrap:break-word;word-break:break-all;box-sizing:border-box;'>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#388e3c;'>Status</b>: <span style='color:").append(getStatusColor(resp.code)).append(";font-weight:bold;'>").append(escapeHtml(String.valueOf(resp.code))).append("</span></div>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>Protocol</b>: <span style='color:").append(textColor).append(";'>").append(escapeHtml(safeString(resp.protocol))).append("</span></div>");
        sb.append("<div style='margin-bottom:8px;word-break:break-all;'><b style='color:#1976d2;'>Thread</b>: <span style='color:").append(textColor).append(";'>").append(escapeHtml(safeString(resp.threadName))).append("</span></div>");
        if (resp.httpEventInfo != null) {
            sb.append("<div style='margin-bottom:8px;word-break:break-all;overflow-wrap:break-word;'><b style='color:#1976d2;'>Connection</b>: <span style='color:").append(textColor).append(";'>").append(escapeHtml(safeString(resp.httpEventInfo.getLocalAddress()))).append(" → ").append(escapeHtml(safeString(resp.httpEventInfo.getRemoteAddress()))).append("</span></div>");
        }
        if (resp.headers != null && !resp.headers.isEmpty()) {
            sb.append("<div style='margin-bottom:8px;'><b style='color:#388e3c;'>Headers</b></div>");
            // 简化表格设计，使用更直接的换行控制
            sb.append("<div style='width:100%;max-width:100%;overflow:hidden;'>");
            resp.headers.forEach((key, values) -> {
                String valueStr = values != null ? String.join(", ", values) : "";
                sb.append("<div style='margin-bottom:4px;padding:4px 8px;background:").append(bgColor).append(";border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>");
                sb.append("<strong style='color:#1976d2;'>").append(escapeHtml(key)).append(":</strong> ");
                sb.append("<span style='color:").append(textColor).append(";'>").append(escapeHtml(valueStr)).append("</span>");
                sb.append("</div>");
            });
            sb.append("</div>");
        }
        sb.append("<div style='margin-bottom:8px;'><b style='color:#388e3c;'>Body</b></div>");
        String responseBody = safeTruncateContent(resp.body);
        // 使用更简单但更有效的换行控制
        sb.append("<pre style='background:").append(bgColor).append(";padding:8px;border-radius:4px;font-size:9px;color:").append(textColor).append(";white-space:pre-wrap;word-break:break-all;overflow-wrap:break-word;width:100%;max-width:100%;margin:0;box-sizing:border-box;overflow:hidden;'>").append(escapeHtml(responseBody)).append("</pre>");
        sb.append("</div>");
        return createHtmlDocument(DETAIL_FONT_SIZE, sb.toString());
    }

    /**
     * 渲染响应信息（整合错误信息 - 扁平化设计）
     * 当有错误时，在响应内容上方显示简洁的错误提示条
     */
    public static String renderResponseWithError(ResultNodeInfo info) {
        if (info == null) {
            return renderResponse(null);
        }
        return buildResponseWithError(info.errorMsg, info.testResults,
                info.resp != null ? info.resp.httpEventInfo : null, info.resp);
    }

    /**
     * 渲染响应信息（整合错误信息 - 针对 RequestResult）
     * 当有错误时，在响应内容上方显示简洁的错误提示条
     */
    public static String renderResponseWithError(RequestResult request) {
        if (request == null) {
            return renderResponse(null);
        }
        return buildResponseWithError(request.getErrorMessage(), request.getTestResults(),
                request.getResponse() != null ? request.getResponse().httpEventInfo : null,
                request.getResponse());
    }

    /**
     * 构建包含错误信息的响应HTML（核心实现）
     *
     * @param errorMessage 错误消息
     * @param testResults  测试结果列表
     * @param eventInfo    HTTP事件信息（用于获取网络错误）
     * @param response     HTTP响应对象
     * @return 完整的HTML文档
     */
    private static String buildResponseWithError(String errorMessage, List<TestResult> testResults,
                                                 HttpEventInfo eventInfo, HttpResponse response) {
        String bgColor = getThemeBackground();
        String textColor = getThemeTextColor();
        StringBuilder sb = new StringBuilder();

        // === 错误信息区域 ===
        appendErrorSection(sb, errorMessage, bgColor, textColor);

        // === 网络错误 ===
        appendNetworkErrorSection(sb, eventInfo, bgColor, textColor);

        // === 标准响应信息 ===
        appendResponseContent(sb, response);

        return createHtmlDocument(DETAIL_FONT_SIZE, sb.toString());
    }

    /**
     * 添加错误信息区域
     */
    private static void appendErrorSection(StringBuilder sb, String errorMessage,
                                           String bgColor, String textColor) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            sb.append("<div style='background:").append(bgColor)
                    .append(";border-left:3px solid ").append(COLOR_ERROR)
                    .append(";padding:10px;margin-bottom:12px;'>");
            sb.append("<div style='color:").append(COLOR_ERROR)
                    .append(";font-weight:bold;font-size:10px;margin-bottom:6px;'>⚠ Error</div>");
            sb.append("<div style='color:").append(textColor)
                    .append(";font-size:9px;white-space:pre-wrap;word-break:break-all;'>")
                    .append(escapeHtml(errorMessage)).append("</div>");
            sb.append("</div>");
        }
    }

    /**
     * 添加网络错误信息区域
     */
    private static void appendNetworkErrorSection(StringBuilder sb, HttpEventInfo eventInfo,
                                                  String bgColor, String textColor) {
        if (eventInfo == null || eventInfo.getErrorMessage() == null || eventInfo.getErrorMessage().isEmpty()) {
            return;
        }

        sb.append("<div style='background:").append(bgColor)
                .append(";border-left:3px solid ").append(COLOR_WARNING)
                .append(";padding:10px;margin-bottom:12px;'>");
        sb.append("<div style='color:").append(COLOR_WARNING)
                .append(";font-weight:bold;font-size:10px;margin-bottom:6px;'>⚠ Network Error</div>");
        sb.append("<div style='color:").append(textColor)
                .append(";font-size:9px;white-space:pre-wrap;word-break:break-all;'>")
                .append(escapeHtml(eventInfo.getErrorMessage())).append("</div>");
        sb.append("</div>");
    }

    /**
     * 添加标准响应内容区域
     */
    private static void appendResponseContent(StringBuilder sb, HttpResponse response) {
        if (response != null) {
            String responseHtml = renderResponse(response);
            int bodyStart = responseHtml.indexOf("<body");
            int bodyEnd = responseHtml.lastIndexOf("</body>");
            if (bodyStart >= 0 && bodyEnd > bodyStart) {
                int contentStart = responseHtml.indexOf('>', bodyStart) + 1;
                sb.append(responseHtml, contentStart, bodyEnd);
            } else {
                sb.append(responseHtml);
            }
        } else {
            sb.append("<div style='color:#888;padding:16px;'>No Response</div>");
        }
    }

    /**
     * 渲染测试结果HTML（支持主题适配）
     */
    public static String renderTestResults(List<TestResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return createHtmlDocument("font-size:9px;", "<div style='color:#888;padding:16px;'>No test results</div>");
        }

        String bgColor = getThemeBackground();
        String textColor = getThemeTextColor();
        String borderColor = getThemeBorderColor();

        StringBuilder table = new StringBuilder()
                .append("<table style='border-collapse:collapse;width:100%;background:").append(bgColor).append(";font-size:9px;border-radius:4px;'>")
                .append("<tr style='").append("color:").append(textColor).append(";font-weight:500;font-size:9px;border-bottom:1px solid ").append(borderColor).append(";'>")
                .append("<th style='padding:8px 12px;text-align:left;'>Name</th>")
                .append("<th style='padding:8px 12px;text-align:center;'>Result</th>")
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
        String bgColor = getThemeBackground();
        String textColor = getThemeTextColor();
        String borderColor = getThemeBorderColor();

        String rowBg = "background:" + bgColor + ";";
        String icon = testResult.passed ? "<span style='color:#4CAF50;font-size:9px;'>&#10003;</span>" : "<span style='color:#F44336;font-size:9px;'>&#10007;</span>";
        String message = testResult.message != null && !testResult.message.isEmpty() ?
                "<div style='color:#F44336;font-size:9px;white-space:pre-wrap;word-break:break-all;'>" + escapeHtml(testResult.message) + "</div>"
                : "";

        return "<tr style='" + rowBg + "border-bottom:1px solid " + borderColor + ";'>" +
                "<td style='padding:8px 12px;color:" + textColor + ";font-size:9px;'>" + escapeHtml(testResult.name) + "</td>" +
                "<td style='padding:8px 12px;text-align:center;vertical-align:middle;'>" + icon + "</td>" +
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
                createTimingRow("Total", calc.getTotal(), COLOR_ERROR, true, "Total time (CallStart→CallEnd, the whole request lifecycle)") +
                createTimingRow("Queueing", calc.getQueueing(), null, false, "Waiting in queue (QueueStart→CallStart, waiting for scheduling)") +
                createTimingRow("Stalled", calc.getStalled(), null, false, "Blocked (CallStart→ConnectStart, includes DNS lookup)") +
                createTimingRow("DNS Lookup", calc.getDns(), null, false, "Domain name lookup (DnsStart→DnsEnd, part of Stalled)") +
                createTimingRow("Initial Connection (TCP)", calc.getConnect(), null, false, "TCP connection setup (ConnectStart→ConnectEnd, includes SSL/TLS)") +
                createTimingRow("SSL/TLS", calc.getTls(), null, false, "SSL/TLS handshake (SecureConnectStart→SecureConnectEnd, part of TCP connection)") +
                createTimingRow("Request Sent", calc.getRequestSent(), null, false, "Sending request headers and body (RequestHeadersStart/RequestBodyStart→RequestHeadersEnd/RequestBodyEnd)") +
                createTimingRow("Waiting (TTFB)", calc.getServerCost(), COLOR_SUCCESS, true, "Server processing time (RequestBodyEnd/RequestHeadersEnd→ResponseHeadersStart)") +
                createTimingRow("Content Download", calc.getResponseBody(), null, false, "Downloading response body (ResponseBodyStart→ResponseBodyEnd)") +
                createTimingRowString("Connection Reused", calc.getConnectionReused() ? "Yes" : "No", null, false, "Was the connection reused for this request?") +
                createTimingRowString("OkHttp Idle Connections", String.valueOf(response.idleConnectionCount), null, false, "Number of idle OkHttp connections (snapshot)") +
                createTimingRowString("OkHttp Total Connections", String.valueOf(response.connectionCount), null, false, "Total number of OkHttp connections (snapshot)") +
                "</table>" +
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

    private static String buildEventInfoHtml(HttpEventInfo info) {
        String bgColor = getThemeBackground();
        String textColor = getThemeTextColor();
        String borderColor = getThemeBorderColor();

        // 改回表格格式，但添加强制换行控制
        return "<div style='width:100%;max-width:100%;overflow-wrap:break-word;box-sizing:border-box;'>" +
                "<div style='font-size:9px;margin-bottom:8px;'><b style='color:" + COLOR_PRIMARY + ";'>[Event Info]</b></div>" +
                // 设置背景色和列间距，添加强制换行
                "<table style='border-collapse:collapse;background:" + bgColor + ";border-radius:4px;padding:3px 4px;color:" + textColor + ";margin:4px 0;width:100%;table-layout:fixed;'>" +
                createEventRow("QueueStart", formatMillis(info.getQueueStart())) +
                createEventRow("Local", escapeHtml(info.getLocalAddress())) +
                createEventRow("Remote", escapeHtml(info.getRemoteAddress())) +
                createEventRow("Protocol", info.getProtocol() != null ? info.getProtocol().toString() : "-") +
                createEventRow("TLS", safeString(info.getTlsVersion())) +
                createEventRow("Thread", safeString(info.getThreadName())) +
                createEventRow("Error", info.getErrorMessage() != null ? escapeHtml(info.getErrorMessage()) : "-") +
                "<tr><td colspan='2'><hr style='border:0;border-top:1px dashed " + borderColor + ";margin:4px 0'></td></tr>" +
                createEventTimingRows(info) +
                "</table>" +
                "</div>";
    }

    private static String createEventRow(String label, String value) {
        // 增加换行控制和固定列宽
        return "<tr><td style='min-width:80px;padding:2px 120px 2px 0;color:" + COLOR_GRAY
                + ";width:30%;word-wrap:break-word;overflow-wrap:break-word;'>"
                + label + "</td><td style='width:70%;word-wrap:break-word;overflow-wrap:break-word;'>" + value + "</td></tr>";
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
        return "<tr><td style='padding:2px 8px 2px 0;" + style + "width:30%;word-wrap:break-word;overflow-wrap:break-word;'>"
                + label + "</td><td style='width:70%;word-wrap:break-word;overflow-wrap:break-word;'>"
                + formatMillis(millis) + "</td></tr>";
    }

    private static String formatMillis(long millis) {
        return millis <= 0 ? "-" : new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(millis));
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
            return truncated + "......\n\n[Content too large, truncated for display. Original length: "
                    + content.length() + " characters, max displayed: " + (MAX_DISPLAY_SIZE / 1024) + "KB]";
        }

        return content;
    }
}
