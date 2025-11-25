package com.laker.postman.service.http;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.*;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.js.JsScriptExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

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

    public static boolean isSSERequest(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        // 判断Accept头是否包含text/event-stream
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("Accept".equalsIgnoreCase(entry.getKey()) &&
                    entry.getValue() != null &&
                    entry.getValue().toLowerCase().contains("text/event-stream")) {
                return true;
            }
        }
        return false;
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


    public static String getMethodColor(String method) {
        String methodColor;
        switch (method == null ? "" : method.toUpperCase()) {
            case "GET" -> methodColor = "#4CAF50";      // GET: 绿色（Postman风格）
            case "POST" -> methodColor = "#FF9800";     // POST: 橙色
            case "PUT" -> methodColor = "#2196F3";      // PUT: 蓝色
            case "PATCH" -> methodColor = "#9C27B0";    // PATCH: 紫色
            case "DELETE" -> methodColor = "#F44336";   // DELETE: 红色
            default -> methodColor = "#7f8c8d";          // 其它: 灰色
        }
        return methodColor;
    }

    // 绑定脚本变量
    public static Map<String, Object> prepareBindings(PreparedRequest req) {
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        Postman postman = new Postman(activeEnv);
        postman.setRequest(req);
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("request", req);
        bindings.put("env", activeEnv);
        bindings.put("postman", postman);
        bindings.put("pm", postman);

        // 为前置脚本提供一个空的响应对象，防止pm.response为undefined
        try {
            // 创建一个空的HttpResponse对象并设置到pm中
            HttpResponse emptyResponse = new HttpResponse();
            emptyResponse.code = 0;
            emptyResponse.headers = new LinkedHashMap<>();
            emptyResponse.body = "{}";
            postman.setResponse(emptyResponse);
            // 添加到bindings中
            bindings.put("response", emptyResponse);
            bindings.put("responseBody", "{}");
            bindings.put("responseHeaders", emptyResponse.headers);
            bindings.put("statusCode", 0);
        } catch (Exception e) {
            log.warn("为前置脚本初始化空响应对象失败", e);
        }

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
                                ConsolePanel.appendLog("[PreScript Console]\n" + output);
                            }
                        }
                );
            } catch (Exception ex) {
                log.error("PreScript Error", ex);
                ConsolePanel.appendLog("[PreScript Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);
                JOptionPane.showMessageDialog(null, "前置脚本执行异常：" + ex.getMessage(), "脚本错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }


    public static void executePostscript(String postscript, Map<String, Object> bindings) {
        // postscript 执行
        if (CharSequenceUtil.isNotBlank(postscript)) {
            try {
                JsScriptExecutor.executeScript(
                        postscript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                ConsolePanel.appendLog("[PostScript Console]\n" + output);
                            }
                        }
                );
                Environment activeEnv = EnvironmentService.getActiveEnvironment();
                if (activeEnv != null) {
                    EnvironmentService.saveEnvironment(activeEnv);
                }
            } catch (Exception ex) {
                log.error("PostScript Error", ex);
                ConsolePanel.appendLog("[PostScript Error]\n" + ex.getMessage(), ConsolePanel.LogType.ERROR);
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
        if (item.getProtocol().isHttpProtocol()
                && req.body != null
                && "GET".equalsIgnoreCase(req.method)
                && item.getBody() != null && !item.getBody().isEmpty()) {
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


    /**
     * 从URL解析参数列表（支持重复参数）
     * 参考Postman的逻辑，允许相同的key出现多次
     *
     * @param url URL字符串
     * @return 参数列表，如果没有参数返回null
     */
    public static List<HttpParam> getParamsListFromUrl(String url) {
        if (url == null) return Collections.emptyList();
        int idx = url.indexOf('?');
        if (idx < 0 || idx == url.length() - 1) return Collections.emptyList();
        String paramStr = url.substring(idx + 1);

        List<HttpParam> urlParams = new ArrayList<>();
        int last = 0;
        while (last < paramStr.length()) {
            int amp = paramStr.indexOf('&', last);
            String pair = (amp == -1) ? paramStr.substring(last) : paramStr.substring(last, amp);
            int eqIdx = pair.indexOf('=');
            String k;
            String v;
            if (eqIdx >= 0) {
                k = pair.substring(0, eqIdx);
                v = pair.substring(eqIdx + 1);
            } else {
                // 处理没有=号的参数，如 &key
                k = pair;
                v = "";
            }

            // 只要key不为空，就添加参数（value可以为空）
            if (CharSequenceUtil.isNotBlank(k)) {
                urlParams.add(new HttpParam(true, k.trim(), v));
            }

            if (amp == -1) break;
            last = amp + 1;
        }

        if (urlParams.isEmpty()) {
            return Collections.emptyList();
        }
        return urlParams;
    }

    /**
     * 获取不带参数的基础URL
     * 例如：https://api.example.com/users?name=john&age=25 -> https://api.example.com/users
     */
    public static String getBaseUrlWithoutParams(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            return url.substring(0, queryIndex);
        }
        return url;
    }

    /**
     * 从参数列表构建完整的URL（支持重复参数，只添加enabled的参数）
     * 参考Postman的逻辑，允许相同的key出现多次
     *
     * @param baseUrl 基础URL（不带参数）
     * @param params  参数列表
     * @return 完整的URL
     */
    public static String buildUrlFromParamsList(String baseUrl, List<HttpParam> params) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return baseUrl;
        }

        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean isFirst = true;

        for (HttpParam param : params) {
            // 只添加enabled且key不为空的参数
            if (param.isEnabled() && CharSequenceUtil.isNotBlank(param.getKey())) {
                if (isFirst) {
                    urlBuilder.append("?");
                    isFirst = false;
                } else {
                    urlBuilder.append("&");
                }

                // 对参数名进行URL编码
                urlBuilder.append(encodeURIComponent(param.getKey()));

                // 如果value不为空，才添加=和value
                if (CharSequenceUtil.isNotBlank(param.getValue())) {
                    urlBuilder.append("=");
                    urlBuilder.append(encodeURIComponent(param.getValue()));
                }
                // 如果value为空，只添加key，不添加=
            }
        }

        return urlBuilder.toString();
    }

    public static String getHeaderIgnoreCase(Map<String, String> headers, String s) {
        if (headers == null || s == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (s.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean isWebSocketRequest(String url) {
        return url != null && (url.startsWith("ws://") || url.startsWith("wss://"));
    }
}