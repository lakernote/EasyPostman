package com.laker.postman.service.render;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.functional.model.RequestResult;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.request.model.HttpFormUrlencoded;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.performance.model.PerformanceInternalHeaders;
import com.laker.postman.performance.model.ResultNodeInfo;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统一的 HTTP 请求/响应 HTML 渲染工具类
 */
@UtilityClass
public class HttpHtmlRenderer {

    private static final int MAX_DISPLAY_SIZE = 2 * 1024;

    // ==================== 字号 ====================

    /**
     * JTextPane HTMLEditorKit 基于 72dpi，Swing 字号基于屏幕 DPI，
     * 乘以 0.7 换算为视觉等效的 HTML px。
     */
    private static int htmlFontSize() {
        return Math.max(8, (int) Math.round(SettingManager.getUiFontSize() * 0.7));
    }

    private static String fs()      { return htmlFontSize() + "px"; }
    private static String fsSmall() { return Math.max(8, htmlFontSize() - 1) + "px"; }

    // ==================== 主题 ====================

    private static String colorPrimary() { return toHex(ModernColors.getPrimary()); }
    private static String colorSuccess() { return toHex(ModernColors.getSuccess()); }
    private static String colorError() { return toHex(ModernColors.getError()); }
    private static String colorWarning() { return toHex(ModernColors.getWarning()); }
    private static String colorGray() { return toHex(ModernColors.getTextHint()); }
    private static String bgColor() { return toHex(ModernColors.getBackgroundColor()); }
    private static String bgColorAlt() { return toHex(ModernColors.getHoverBackgroundColor()); }
    private static String textColor() { return toHex(ModernColors.getTextPrimary()); }
    private static String borderColor() { return toHex(ModernColors.getBorderLightColor()); }
    private static String codeBgColor() { return toHex(ModernColors.getHoverBackgroundColor()); }

    private static String statusColor(int code) {
        if (code <= 0) return colorError();
        if (code >= 500) return colorError();
        if (code >= 400) return colorWarning();
        return colorSuccess();
    }

    // ==================== HTML 文档 ====================

    /**
     * 完整 HTML 文档：body 设好字体、字号、颜色，子元素直接继承，无需重复设置。
     */
    private static String htmlDoc(String bodyContent) {
        return "<html><body style='"
                + "font-family:monospace;"
                + "font-size:" + fs() + ";"
                + "color:" + textColor() + ";"
                + "margin:6px;"
                + "'>" + bodyContent + "</body></html>";
    }

    // ==================== 通用 HTML 片段 ====================

    /** key: value 行，支持斑马纹 */
    private static String kvRow(String keyColor, String key, String value, boolean alt) {
        return "<div style='padding:3px 8px;background:" + (alt ? bgColorAlt() : bgColor())
                + ";border-radius:3px;margin-bottom:2px;word-break:break-all;'>"
                + "<span style='color:" + keyColor + ";font-weight:bold;'>" + key + "</span>"
                + "<span style='color:" + colorGray() + ";'> : </span>"
                + "<span>" + value + "</span>"
                + "</div>";
    }

    /** 节标题，带左边竖线装饰 */
    private static String sectionTitle(String color, String title) {
        return "<div style='margin:10px 0 4px 0;padding-left:6px;"
                + "border-left:3px solid " + color + ";"
                + "font-weight:bold;color:" + color + ";'>" + title + "</div>";
    }

    /** 无数据提示 */
    private static String noData(String message) {
        return "<div style='color:" + colorGray() + ";padding:12px;font-style:italic;'>" + message + "</div>";
    }

    /** 警告/错误提示框 */
    private static String alertBox(String color, String title, String message) {
        return "<div style='border-left:3px solid " + color + ";padding:8px 12px;"
                + "margin-bottom:10px;background:" + bgColor() + ";border-radius:0 4px 4px 0;'>"
                + "<div style='color:" + color + ";font-weight:bold;margin-bottom:4px;'>" + escapeHtml(title) + "</div>"
                + "<div style='white-space:pre-wrap;word-break:break-all;'>" + escapeHtml(message) + "</div>"
                + "</div>";
    }

    // ==================== 公开 API ====================

    public static String renderTimingInfo(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) return htmlDoc(noData("No Timing Info"));
        return htmlDoc(buildTimingHtml(response));
    }

    public static String renderEventInfo(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) return htmlDoc(noData("No Event Info"));
        return htmlDoc(buildEventInfoHtml(response.httpEventInfo));
    }

    /** 渲染请求信息 */
    public static String renderRequest(PreparedRequest req) {
        if (req == null) return htmlDoc(noData("无请求信息"));
        StringBuilder sb = new StringBuilder();

        sb.append(kvRow(colorPrimary(), "URL",    escapeHtml(safeStr(req.url)),    false));
        sb.append(kvRow(colorPrimary(), "Method", escapeHtml(safeStr(req.method)), true));

        if (req.okHttpHeaders != null && req.okHttpHeaders.size() > 0) {
            sb.append(sectionTitle(colorPrimary(), "Headers"));
            for (int i = 0; i < req.okHttpHeaders.size(); i++) {
                sb.append(kvRow(colorPrimary(),
                        escapeHtml(req.okHttpHeaders.name(i)),
                        escapeHtml(req.okHttpHeaders.value(i)), i % 2 != 0));
            }
        }

        if (req.formDataList != null && !req.formDataList.isEmpty()) {
            boolean hasText = req.formDataList.stream().anyMatch(d -> d.isEnabled() && d.isText());
            boolean hasFile = req.formDataList.stream().anyMatch(d -> d.isEnabled() && d.isFile());
            if (hasText) {
                sb.append(sectionTitle(colorPrimary(), "Form Data"));
                int[] idx = {0};
                req.formDataList.stream().filter(d -> d.isEnabled() && d.isText()).forEach(d ->
                        sb.append(kvRow(colorPrimary(), escapeHtml(d.getKey()), escapeHtml(d.getValue()), idx[0]++ % 2 != 0)));
            }
            if (hasFile) {
                sb.append(sectionTitle(colorPrimary(), "Form Files"));
                int[] idx = {0};
                req.formDataList.stream().filter(d -> d.isEnabled() && d.isFile()).forEach(d ->
                        sb.append(kvRow(colorPrimary(), escapeHtml(d.getKey()), escapeHtml(d.getValue()), idx[0]++ % 2 != 0)));
            }
        }

        if (req.urlencodedList != null && !req.urlencodedList.isEmpty()) {
            sb.append(sectionTitle(colorPrimary(), "x-www-form-urlencoded"));
            int[] idx = {0};
            req.urlencodedList.stream().filter(HttpFormUrlencoded::isEnabled).forEach(e ->
                    sb.append(kvRow(colorPrimary(), escapeHtml(e.getKey()), escapeHtml(e.getValue()), idx[0]++ % 2 != 0)));
        }

        if (isNotEmpty(req.okHttpRequestBody)) {
            sb.append(sectionTitle(colorPrimary(), "Body"));
            sb.append(codeBlock(truncate(req.okHttpRequestBody)));
        }

        return htmlDoc(sb.toString());
    }

    /** 渲染响应信息 */
    public static String renderResponse(HttpResponse resp) {
        if (resp == null) return htmlDoc(noData("无响应信息"));
        StringBuilder sb = new StringBuilder();

        int code = resp.code;
        String statusBadge = "<span style='color:" + statusColor(code) + ";font-weight:bold;padding:1px 6px;"
                + "border:1px solid " + statusColor(code) + ";border-radius:3px;'>" + code + "</span>";
        sb.append(kvRow(colorSuccess(), "Status",   statusBadge, false));
        sb.append(kvRow(colorPrimary(), "Protocol", escapeHtml(safeStr(resp.protocol)),   true));
        sb.append(kvRow(colorPrimary(), "Thread",   escapeHtml(safeStr(resp.threadName)), false));
        if (resp.httpEventInfo != null) {
            sb.append(kvRow(colorPrimary(), "Connection",
                    escapeHtml(safeStr(resp.httpEventInfo.getLocalAddress()))
                            + " <span style='color:" + colorGray() + ";'>→</span> "
                            + escapeHtml(safeStr(resp.httpEventInfo.getRemoteAddress())), true));
        }

        appendResponseHeaders(sb, resp.headers);

        sb.append(sectionTitle(colorSuccess(), "Body"));
        sb.append(codeBlock(truncate(resp.body)));

        return htmlDoc(sb.toString());
    }

    public static String renderResponseWithError(ResultNodeInfo info) {
        if (info == null) return renderResponse(null);
        return buildResponseWithError(info.errorMsg,
                info.resp != null ? info.resp.httpEventInfo : null, info.resp);
    }

    public static String renderResponseWithError(RequestResult request) {
        if (request == null) return renderResponse(null);
        return buildResponseWithError(request.getErrorMessage(),
                request.getResponse() != null ? request.getResponse().httpEventInfo : null,
                request.getResponse());
    }

    /** 渲染测试结果 */
    public static String renderTestResults(List<TestResult> testResults) {
        if (testResults == null || testResults.isEmpty()) return htmlDoc(noData("No test results"));

        StringBuilder sb = new StringBuilder();
        sb.append("<table style='border-collapse:collapse;width:100%;'>")
                .append("<tr style='font-weight:bold;border-bottom:2px solid ").append(borderColor()).append(";'>")
                .append("<th style='padding:5px 10px;text-align:left;'>Name</th>")
                .append("<th style='padding:5px 10px;text-align:center;width:60px;'>Result</th>")
                .append("<th style='padding:5px 10px;text-align:left;'>Message</th>")
                .append("</tr>");
        int[] idx = {0};
        for (TestResult r : testResults) {
            if (r != null) sb.append(testResultRow(r, idx[0]++ % 2 != 0));
        }
        sb.append("</table>");
        return htmlDoc(sb.toString());
    }

    // ==================== 私有实现 ====================

    /** 代码块（请求/响应 body） */
    private static String codeBlock(String content) {
        return "<div style='background:" + codeBgColor()
                + ";color:" + textColor()
                + ";padding:8px;border-radius:4px;margin:2px 0 6px 0;'>"
                + "<pre style='margin:0;font-family:monospace;white-space:pre-wrap;word-break:break-all;'>"
                + escapeHtml(content)
                + "</pre></div>";
    }

    /**
     * 构建包含错误信息的响应内容（不做字符串截取，直接复用 renderResponseBody）
     */
    private static String buildResponseWithError(String errorMsg, HttpEventInfo eventInfo, HttpResponse response) {
        StringBuilder sb = new StringBuilder();
        if (isNotEmpty(errorMsg)) {
            sb.append(alertBox(colorError(), "⚠ Error", errorMsg));
        }
        if (eventInfo != null && isNotEmpty(eventInfo.getErrorMessage())) {
            sb.append(alertBox(colorWarning(), "⚠ Network Error", eventInfo.getErrorMessage()));
        }
        // 直接拼响应内容片段，不包装外层 htmlDoc（由最后统一包装）
        sb.append(renderResponseBody(response));
        return htmlDoc(sb.toString());
    }

    /**
     * 只输出响应内容片段（无 htmlDoc 包装），供 buildResponseWithError 复用
     */
    private static String renderResponseBody(HttpResponse resp) {
        if (resp == null) return noData("No Response");
        StringBuilder sb = new StringBuilder();

        int code = resp.code;
        String statusBadge = "<span style='color:" + statusColor(code) + ";font-weight:bold;padding:1px 6px;"
                + "border:1px solid " + statusColor(code) + ";border-radius:3px;'>" + code + "</span>";
        sb.append(kvRow(colorSuccess(), "Status",   statusBadge, false));
        sb.append(kvRow(colorPrimary(), "Protocol", escapeHtml(safeStr(resp.protocol)),   true));
        sb.append(kvRow(colorPrimary(), "Thread",   escapeHtml(safeStr(resp.threadName)), false));
        if (resp.httpEventInfo != null) {
            sb.append(kvRow(colorPrimary(), "Connection",
                    escapeHtml(safeStr(resp.httpEventInfo.getLocalAddress()))
                            + " <span style='color:" + colorGray() + ";'>→</span> "
                            + escapeHtml(safeStr(resp.httpEventInfo.getRemoteAddress())), true));
        }
        appendResponseHeaders(sb, resp.headers);
        sb.append(sectionTitle(colorSuccess(), "Body"));
        sb.append(codeBlock(truncate(resp.body)));
        return sb.toString();
    }

    private static void appendResponseHeaders(StringBuilder sb, Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        boolean hasVisibleHeader = headers.keySet().stream()
                .anyMatch(name -> !PerformanceInternalHeaders.isInternalHeader(name));
        if (!hasVisibleHeader) {
            return;
        }
        sb.append(sectionTitle(colorSuccess(), "Headers"));
        int[] idx = {0};
        headers.forEach((key, values) -> {
            if (PerformanceInternalHeaders.isInternalHeader(key)) {
                return;
            }
            sb.append(kvRow(colorPrimary(), escapeHtml(key),
                    escapeHtml(values != null ? String.join(", ", values) : ""),
                    idx[0]++ % 2 != 0));
        });
    }

    private static String testResultRow(TestResult r, boolean alt) {
        String icon = r.passed
                ? "<span style='color:" + colorSuccess() + ";font-size:" + (htmlFontSize() + 1) + "px;'>&#10003;</span>"
                : "<span style='color:" + colorError() + ";font-size:" + (htmlFontSize() + 1) + "px;'>&#10007;</span>";
        String msg = isNotEmpty(r.message)
                ? "<span style='color:" + colorError() + ";white-space:pre-wrap;word-break:break-all;'>" + escapeHtml(r.message) + "</span>"
                : "";
        String bg = alt ? bgColorAlt() : bgColor();
        return "<tr style='background:" + bg + ";border-bottom:1px solid " + borderColor() + ";'>"
                + "<td style='padding:5px 10px;'>" + escapeHtml(r.name) + "</td>"
                + "<td style='padding:5px 10px;text-align:center;'>" + icon + "</td>"
                + "<td style='padding:5px 10px;'>" + msg + "</td>"
                + "</tr>";
    }

    // Timeline 各阶段语义色（参考 Chrome DevTools Network 面板配色）
    private static final String COLOR_T_QUEUE    = "#9e9e9e"; // 灰   - 排队/阻塞
    private static final String COLOR_T_DNS      = "#009688"; // 青绿 - DNS 解析
    private static final String COLOR_T_TCP      = "#e67c00"; // 深橙 - TCP 连接
    private static final String COLOR_T_SSL      = "#8e24aa"; // 紫   - SSL/TLS
    private static final String COLOR_T_REQUEST  = "#1565c0"; // 深蓝 - 发送请求
    private static final String COLOR_T_TTFB     = "#2e7d32"; // 深绿 - 等待响应(TTFB)
    private static final String COLOR_T_DOWNLOAD = "#00838f"; // 青   - 下载响应体

    private static String buildTimingHtml(HttpResponse response) {
        HttpEventInfo info = response.httpEventInfo;
        TimingCalculator calc = new TimingCalculator(info);
        long total = calc.getTotal();

        StringBuilder sb = new StringBuilder();
        sb.append(sectionTitle(colorPrimary(), "Timeline"));
        sb.append("<table style='border-collapse:collapse;width:100%;table-layout:fixed;'>");
        sb.append("<tr style='font-weight:bold;border-bottom:2px solid ").append(borderColor()).append(";color:").append(colorGray()).append(";'>")
                .append("<th style='padding:4px 6px;text-align:left;width:30%;'>Phase</th>")
                .append("<th style='padding:4px 6px;text-align:right;width:16%;'>Time</th>")
                .append("<th style='padding:4px 6px;width:54%;'>Bar</th>")
                .append("</tr>");

        // Total 行不显示 bar（它是基准，显示 100% bar 没意义）
        timingRow(sb, "Total",               calc.getTotal(),        colorError(),   true,  total, true);
        timingRow(sb, "Queueing",            calc.getQueueing(),     COLOR_T_QUEUE,  false, total, false);
        timingRow(sb, "Stalled",             calc.getStalled(),      COLOR_T_QUEUE,  false, total, false);
        timingRow(sb, "  ↳ DNS Lookup",      calc.getDns(),          COLOR_T_DNS,    false, total, false);
        timingRow(sb, "TCP Connection",      calc.getConnect(),      COLOR_T_TCP,    false, total, false);
        timingRow(sb, "  ↳ SSL/TLS",         calc.getTls(),          COLOR_T_SSL,    false, total, false);
        timingRow(sb, "Request Sent",        calc.getRequestSent(),  COLOR_T_REQUEST,false, total, false);
        timingRow(sb, "Waiting (TTFB)",      calc.getServerCost(),   COLOR_T_TTFB,   true,  total, false);
        timingRow(sb, "Content Download",    calc.getResponseBody(), COLOR_T_DOWNLOAD,false,total, false);

        sb.append("<tr><td colspan='3'><hr style='border:0;border-top:1px dashed ").append(borderColor()).append(";margin:4px 0'/></td></tr>");
        appendTimingRow(sb, "Connection Reused", calc.getConnectionReused() ? "Yes" : "No", null, false, -1, total);
        appendTimingRow(sb, "Idle Connections",  String.valueOf(response.idleConnectionCount), null, false, -1, total);
        appendTimingRow(sb, "Total Connections", String.valueOf(response.connectionCount),     null, false, -1, total);

        sb.append("</table>");
        return sb.toString();
    }

    private static void timingRow(StringBuilder sb, String name, long val, String color, boolean bold, long total, boolean hideBar) {
        appendTimingRow(sb, name, val >= 0 ? val + " ms" : "-", color, bold,
                hideBar ? -1 : (val > 0 ? val : -1), total);
    }

    /**
     * Timeline 行：名称列 + 时间列 + 进度条列
     * 用 table 实现进度条——JTextPane HTML 渲染器对嵌套 div 的 width:% 支持很差，
     * 而 table 的 width 属性支持可靠。
     */
    private static void appendTimingRow(StringBuilder sb, String name, String val,
                                        String color, boolean bold, long barVal, long total) {
        String nameStyle = (bold ? "font-weight:bold;" : "")
                + (color != null ? "color:" + color + ";" : "color:" + textColor() + ";");
        String valStyle = (color != null ? "color:" + color + ";" : "color:" + textColor() + ";")
                + (bold ? "font-weight:bold;" : "");

        // 进度条：用 table 实现，两列：filled + empty，宽度用整数 px 近似
        String bar = "";
        if (barVal > 0 && total > 0) {
            int pct = (int) Math.min(100, Math.round(barVal * 100.0 / total));
            int emptyPct = 100 - pct;
            String barColor = color != null ? color : colorPrimary();
            // 用 table 宽度百分比：JTextPane 对 table width=% 支持良好
            bar = "<table style='border-collapse:collapse;width:100%;' cellpadding='0' cellspacing='0'><tr>"
                    + "<td width='" + pct + "%' style='background:" + barColor
                    + ";height:8px;border-radius:2px 0 0 2px;'></td>"
                    + (emptyPct > 0 ? "<td width='" + emptyPct + "%' style='background:" + borderColor() + ";height:8px;'></td>" : "")
                    + "</tr></table>"
                    + "<span style='color:" + colorGray() + ";font-size:" + fsSmall() + ";'>" + pct + "%</span>";
        }

        sb.append("<tr style='border-bottom:1px solid ").append(borderColor()).append(";'>")
                .append("<td style='padding:3px 6px;").append(nameStyle).append("'>").append(name).append("</td>")
                .append("<td style='padding:3px 6px;text-align:right;").append(valStyle).append("'>").append(val).append("</td>")
                .append("<td style='padding:3px 6px;'>").append(bar).append("</td>")
                .append("</tr>");
    }

    private static String buildEventInfoHtml(HttpEventInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(sectionTitle(colorPrimary(), "Summary"));
        sb.append("<table style='border-collapse:collapse;width:100%;margin-bottom:8px;'>");
        eventRow(sb, "Local Address",  escapeHtml(info.getLocalAddress()),  false);
        eventRow(sb, "Remote Address", escapeHtml(info.getRemoteAddress()),  true);
        eventRow(sb, "Protocol",       info.getProtocol() != null ? info.getProtocol().toString() : "-", false);
        eventRow(sb, "TLS Version",    safeStr(info.getTlsVersion()),        true);
        eventRow(sb, "Thread",         safeStr(info.getThreadName()),        false);
        if (isNotEmpty(info.getErrorMessage())) {
            eventRow(sb, "Error", "<span style='color:" + colorError() + ";'>" + escapeHtml(info.getErrorMessage()) + "</span>", true);
        }
        sb.append("</table>");

        sb.append(sectionTitle(colorPrimary(), "Event Timestamps"));
        sb.append("<table style='border-collapse:collapse;width:100%;'>");
        sb.append("<tr style='font-weight:bold;border-bottom:2px solid ").append(borderColor()).append(";color:").append(colorGray()).append(";'>")
                .append("<th style='padding:3px 8px;text-align:left;width:40%;'>Event</th>")
                .append("<th style='padding:3px 8px;text-align:left;'>Time</th>")
                .append("</tr>");

        // 只显示非空（>0）的时间戳，减少噪音
        appendEventTimingRowIfSet(sb, "QueueStart",          info.getQueueStart(),          colorGray(),    false);
        appendEventTimingRowIfSet(sb, "CallStart",            info.getCallStart(),            colorPrimary(), true);
        appendEventTimingRowIfSet(sb, "DnsStart",             info.getDnsStart(),             null,          false);
        appendEventTimingRowIfSet(sb, "DnsEnd",               info.getDnsEnd(),               null,          true);
        appendEventTimingRowIfSet(sb, "ConnectStart",         info.getConnectStart(),         null,          false);
        appendEventTimingRowIfSet(sb, "SecureConnectStart",   info.getSecureConnectStart(),   null,          true);
        appendEventTimingRowIfSet(sb, "SecureConnectEnd",     info.getSecureConnectEnd(),     null,          false);
        appendEventTimingRowIfSet(sb, "ConnectEnd",           info.getConnectEnd(),           null,          true);
        appendEventTimingRowIfSet(sb, "ConnectionAcquired",   info.getConnectionAcquired(),   colorPrimary(), false);
        appendEventTimingRowIfSet(sb, "RequestHeadersStart",  info.getRequestHeadersStart(),  null,          true);
        appendEventTimingRowIfSet(sb, "RequestHeadersEnd",    info.getRequestHeadersEnd(),    null,          false);
        appendEventTimingRowIfSet(sb, "RequestBodyStart",     info.getRequestBodyStart(),     null,          true);
        appendEventTimingRowIfSet(sb, "RequestBodyEnd",       info.getRequestBodyEnd(),       null,          false);
        appendEventTimingRowIfSet(sb, "ResponseHeadersStart", info.getResponseHeadersStart(), colorSuccess(), true);
        appendEventTimingRowIfSet(sb, "ResponseHeadersEnd",   info.getResponseHeadersEnd(),   null,          false);
        appendEventTimingRowIfSet(sb, "ResponseBodyStart",    info.getResponseBodyStart(),    null,          true);
        appendEventTimingRowIfSet(sb, "ResponseBodyEnd",      info.getResponseBodyEnd(),      null,          false);
        appendEventTimingRowIfSet(sb, "ConnectionReleased",   info.getConnectionReleased(),   null,          true);
        appendEventTimingRowIfSet(sb, "CallEnd",              info.getCallEnd(),              colorPrimary(), false);
        appendEventTimingRowIfSet(sb, "CallFailed",           info.getCallFailed(),           colorError(),   true);
        appendEventTimingRowIfSet(sb, "Canceled",             info.getCanceled(),             colorError(),   false);

        sb.append("</table>");
        return sb.toString();
    }

    private static void eventRow(StringBuilder sb, String label, String value, boolean alt) {
        sb.append("<tr style='background:").append(alt ? bgColorAlt() : bgColor()).append(";'>")
                .append("<td style='width:35%;color:").append(colorGray()).append(";padding:3px 8px;'>").append(label).append("</td>")
                .append("<td style='width:65%;padding:3px 8px;word-break:break-all;'>").append(value).append("</td>")
                .append("</tr>");
    }

    /** 只在时间戳 > 0 时才输出行，避免大量 "-" 噪音 */
    private static void appendEventTimingRowIfSet(StringBuilder sb, String label, long millis, String color, boolean alt) {
        if (millis <= 0) return;
        String style = color != null ? "color:" + color + ";" : "";
        sb.append("<tr style='background:").append(alt ? bgColorAlt() : bgColor()).append(";border-bottom:1px solid ").append(borderColor()).append(";'>")
                .append("<td style='padding:3px 8px;").append(style).append("width:40%;'>").append(label).append("</td>")
                .append("<td style='padding:3px 8px;width:60%;'>").append(formatMillis(millis)).append("</td>")
                .append("</tr>");
    }

    // ==================== 工具方法 ====================

    private static String safeStr(String s) { return s != null ? s : "-"; }
    private static boolean isNotEmpty(String s) { return s != null && !s.isEmpty(); }

    private static String formatMillis(long millis) {
        return millis <= 0 ? "-" : new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(millis));
    }

    private static String truncate(String content) {
        if (content == null) return "";
        if (content.length() <= MAX_DISPLAY_SIZE) return content;
        return content.substring(0, MAX_DISPLAY_SIZE)
                + "\n\n[Truncated: " + content.length() + " chars, showing first " + (MAX_DISPLAY_SIZE / 1024) + "KB]";
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String toHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
