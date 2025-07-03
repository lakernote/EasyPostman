package com.laker.postman.service.http;

import cn.hutool.core.util.StrUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.js.JsScriptExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class HttpUtil {

    /**
     * Postman 的行为是：对于 params，只有空格和部分特殊字符会被编码，像 : 这样的字符不会被编码。
     * 只对空格、&、=、?、# 这些必须编码的字符进行编码，: 不编码。
     * 对已编码的 %XX 不再重复编码。
     */
    public static String encodeURIComponent(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            // 检查是否为已编码的 %XX
            if (c == '%' && i + 2 < s.length()
                    && isHexChar(s.charAt(i + 1))
                    && isHexChar(s.charAt(i + 2))) {
                sb.append(s, i, i + 3);
                i += 3;
            } else if (c == ' ') {
                sb.append("%20");
                i++;
            } else if (c == '&' || c == '=' || c == '?' || c == '#') {
                sb.append(String.format("%%%02X", (int) c));
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }


    /**
     * 判断headers中是否已包含指定Content-Type（忽略大小写和内容）
     */
    public static boolean containsContentType(Map<String, String> headers, String targetType) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("Content-Type".equalsIgnoreCase(entry.getKey()) &&
                    entry.getValue() != null &&
                    entry.getValue().toLowerCase().contains(targetType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将字节数转换为人类可读的格式
     *
     * @param bytes 字节数
     * @return 格式化后的字符串，例如 "1.5 MB" 或 "512 KB"
     */
    public static String getSizeText(int bytes) {
        String sizeText = "";
        if (bytes < 1024) {
            sizeText = String.format("%d B", bytes);
        } else if (bytes < 1024 * 1024) {
            sizeText = String.format("%.1f KB", bytes / 1024.0);
        } else {
            sizeText = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return sizeText;
    }

    // 判断是否为SSE请求
    public static boolean isSSERequest(PreparedRequest req) {
        if (req == null || req.headers == null) {
            return false;
        }
        // 判断Accept头是否包含text/event-stream
        for (Map.Entry<String, String> entry : req.headers.entrySet()) {
            if ("Accept".equalsIgnoreCase(entry.getKey()) &&
                    entry.getValue() != null &&
                    entry.getValue().toLowerCase().contains("text/event-stream")) {
                return true;
            }
        }
        return false;
    }


    public static boolean isWebSocketRequest(PreparedRequest req) {
        return StrUtil.startWithAny(req.url, "ws://", "wss://");
    }

    // 状态颜色
    public static Color getStatusColor(int statusCode) {
        if (statusCode == 101) {
            return new Color(0, 150, 0);
        }
        if (statusCode >= 200 && statusCode < 300) {
            return new Color(0, 150, 0);
        } else if (statusCode >= 400 && statusCode < 500) {
            return new Color(230, 130, 0);
        } else if (statusCode >= 500) {
            return new Color(200, 0, 0);
        } else if (statusCode >= 300) {
            return new Color(0, 120, 200);
        } else {
            return Color.RED;
        }
    }

    // 绑定脚本变量
    public static Map<String, Object> prepareBindings(PreparedRequest req) {
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        Postman postman = new Postman(activeEnv);
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("request", req);
        bindings.put("env", activeEnv);
        bindings.put("postman", postman);
        bindings.put("pm", postman);
        return bindings;
    }


    public static void postBindings(Map<String, Object> bindings, HttpResponse resp) {
        Postman pm = (Postman) bindings.get("pm");
        pm.setResponse(resp);
        bindings.put("response", resp);
        bindings.put("responseBody", resp.body);
        bindings.put("responseHeaders", resp.headers);
        bindings.put("statusCode", resp.code);
    }


    // 执行前置脚本，异常时弹窗并返回false
    public static boolean executePrescript(HttpRequestItem item, Map<String, Object> bindings) {
        String prescript = item.getPrescript();
        if (prescript != null && !prescript.isBlank()) {
            try {
                JsScriptExecutor.executeScript(
                        prescript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                SidebarTabPanel.appendConsoleLog("[PreScript Console]\n" + output);
                            }
                        }
                );
            } catch (Exception ex) {
                log.error("前置脚本执行异常: {}", ex.getMessage(), ex);
                SidebarTabPanel.appendConsoleLog("[PreScript Error] " + ex.getMessage());
                JOptionPane.showMessageDialog(null, "前置脚本执行异常：" + ex.getMessage(), "脚本错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }


    public static void executePostscript(HttpRequestItem item, Map<String, Object> bindings, HttpResponse resp, String bodyText) {
        // postscript 执行
        String postscript = item.getPostscript();
        if (postscript != null && !postscript.isBlank() && resp != null) {
            try {
                JsScriptExecutor.executeScript(
                        postscript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                SidebarTabPanel.appendConsoleLog("[PostScript Console]\n" + output);
                            }
                        }
                );
                Environment activeEnv = EnvironmentService.getActiveEnvironment();
                if (activeEnv != null) {
                    EnvironmentService.saveEnvironment(activeEnv);
                    SingletonFactory.getInstance(EnvironmentPanel.class).refreshUI();
                }
            } catch (Exception ex) {
                log.error("后置脚本执行异常: {}", ex.getMessage(), ex);
                SidebarTabPanel.appendConsoleLog("[PostScript Error] " + ex.getMessage());
                JOptionPane.showMessageDialog(null, "后置脚本执行异常：" + ex.getMessage(), "脚本错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 校验请求参数
    public static boolean validateRequest(PreparedRequest req, HttpRequestItem item) {
        if (req.url.isEmpty()) {
            JOptionPane.showMessageDialog(null, "请输入有效的 URL");
            return false;
        }
        if (req.method == null || req.method.isEmpty()) {
            JOptionPane.showMessageDialog(null, "请选择请求方法");
            return false;
        }
        if (req.body != null && "GET".equalsIgnoreCase(req.method) && item.getBody() != null && !item.getBody().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "GET 请求通常不包含请求体，是否继续发送？",
                    "确认",
                    JOptionPane.YES_NO_OPTION
            );
            return confirm == JOptionPane.YES_OPTION;
        }
        return true;
    }


    public static String highlightJson(String json) {
        String s = json.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        s = s.replaceAll("(\"[^\"]+\")\\s*:", "<span style='color:#1565c0;'>$1</span>:"); // 保持兼容，Java正则里\\"
        s = s.replaceAll(":\\s*(\".*?\")", ": <span style='color:#43a047;'>$1</span>");
        s = s.replaceAll(":\\s*([\\d.eE+-]+)", ": <span style='color:#8e24aa;'>$1</span>");
        return s;
    }


    public static Map<String, String> getMergedParams(Map<String, String> params, String url) {
        Map<String, String> urlParams = new LinkedHashMap<>();
        if (url != null && url.contains("?")) {
            int idx = url.indexOf('?');
            String paramStr = url.substring(idx + 1);
            // 拆解参数并urldecode
            int last = 0;
            while (last < paramStr.length()) {
                int amp = paramStr.indexOf('&', last);
                String pair = (amp == -1) ? paramStr.substring(last) : paramStr.substring(last, amp);
                int eqIdx = pair.indexOf('=');
                if (eqIdx > 0) {
                    String k = pair.substring(0, eqIdx);
                    String v = pair.substring(eqIdx + 1);
                    urlParams.put(k, v);
                } else if (!pair.isEmpty()) {
                    urlParams.put(pair, "");
                }
                if (amp == -1) break;
                last = amp + 1;
            }
        }
        // 合并 params，item.getParams() 优先生效
        Map<String, String> mergedParams = new LinkedHashMap<>(urlParams);
        if (params != null) {
            mergedParams.putAll(params);
        }
        return mergedParams;
    }


    public static Map<String, String> getParamsMapFromUrl(String url) {
        if (url == null) return null;
        int idx = url.indexOf('?');
        if (idx < 0 || idx == url.length() - 1) return null;
        String paramStr = url.substring(idx + 1);
        if (!paramStr.contains("=")) return null;
        Map<String, String> urlParams = new LinkedHashMap<>();
        int last = 0;
        while (last < paramStr.length()) {
            int amp = paramStr.indexOf('&', last);
            String pair = (amp == -1) ? paramStr.substring(last) : paramStr.substring(last, amp);
            int eqIdx = pair.indexOf('=');
            String k, v;
            if (eqIdx >= 0) {
                k = pair.substring(0, eqIdx);
                v = pair.substring(eqIdx + 1);
            } else {
                // 没有=号的pair不处理
                last = (amp == -1) ? paramStr.length() : amp + 1;
                continue;
            }
            if (StrUtil.isNotBlank(k) && StrUtil.isNotBlank(v)) {
                urlParams.put(k, v);
            }
            if (amp == -1) break;
            last = amp + 1;
        }
        if (urlParams.isEmpty()) {
            return null;
        }
        return urlParams;
    }


    /**
     * 如果 headers 中不包含指定 Content-Type，则补充，并同步更新 headersPanel
     */
    public static void ensureContentTypeHeader(Map<String, String> headers, String contentType, EasyNameValueTablePanel headersPanel) {
        if (!HttpUtil.containsContentType(headers, contentType)) {
            headers.put("Content-Type", contentType);
            headersPanel.addRow("Content-Type", contentType);
        }
    }
}
