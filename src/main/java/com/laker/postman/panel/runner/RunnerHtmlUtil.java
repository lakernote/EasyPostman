package com.laker.postman.panel.runner;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;

import java.util.List;
import java.util.Map;

public class RunnerHtmlUtil {
    public static String buildRequestHtml(PreparedRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:10px;'>");
        sb.append("<b>URL:</b> ").append(escapeHtml(req.url)).append("<br/>");
        sb.append("<b>方法:</b> ").append(escapeHtml(req.method)).append("<br/>");
        // 优先展示 okHttpHeaders，兼容多值和顺序
        if (req.okHttpHeaders != null && req.okHttpHeaders.size() > 0) {
            sb.append("<b>请求头:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (int i = 0; i < req.okHttpHeaders.size(); i++) {
                String name = req.okHttpHeaders.name(i);
                String value = req.okHttpHeaders.value(i);
                sb.append("<tr><td>").append(escapeHtml(name)).append(":</td><td>")
                        .append(escapeHtml(value)).append("</td></tr>");
            }
            sb.append("</table>");
        }
        // 优先展示真实OkHttp请求体内容
        if (req.okHttpRequestBody != null && !req.okHttpRequestBody.isEmpty()) {
            sb.append("<b>真实请求体:</b><br/><pre style='background:#f8f8f8;border:1px solid #eee;padding:8px;'>")
                    .append(escapeHtml(req.okHttpRequestBody)).append("</pre>");
        }
        if (req.body != null && !req.body.isEmpty()) {
            sb.append("<b>请求体:</b><br/><pre style='background:#f8f8f8;border:1px solid #eee;padding:8px;'>")
                    .append(escapeHtml(req.body)).append("</pre>");
        }
        if (req.formData != null && !req.formData.isEmpty()) {
            sb.append("<b>Form Data:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.formData.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        if (req.formFiles != null && !req.formFiles.isEmpty()) {
            sb.append("<b>Form Files:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.formFiles.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        if (req.urlencoded != null && !req.urlencoded.isEmpty()) {
            sb.append("<b>x-www-form-urlencoded:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
            for (Map.Entry<String, String> entry : req.urlencoded.entrySet()) {
                sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>")
                        .append(escapeHtml(entry.getValue())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public static String buildResponseHtml(HttpResponse resp) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:10px;'>");
        if (resp == null) {
            sb.append("<span style='color:gray;'>无响应信息</span>");
        } else {
            sb.append("<b>状态码:</b> ").append(resp.code).append("<br/>");
            if (resp.headers != null && !resp.headers.isEmpty()) {
                sb.append("<b>响应头:</b><br/><table border='1' cellspacing='0' cellpadding='3'>");
                for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                    sb.append("<tr><td>").append(escapeHtml(entry.getKey())).append(":</td><td>");
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        for (int i = 0; i < values.size(); i++) {
                            sb.append(escapeHtml(values.get(i)));
                            if (i < values.size() - 1) sb.append(", ");
                        }
                    }
                    sb.append("</td></tr>");
                }
                sb.append("</table>");
            }
            sb.append("<b>响应体:</b><br/><pre style='background:#f8f8f8;border:1px solid #eee;padding:8px;'>")
                    .append(resp.body != null ? escapeHtml(resp.body) : "<无响应体>")
                    .append("</pre>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public static String buildTestsHtml(List<?> testResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:monospace;font-size:10px;'>");
        sb.append("<table border='1' cellspacing='0' cellpadding='6'>");
        sb.append("<tr><th>名称</th><th>结果</th><th>异常信息</th></tr>");
        for (Object obj : testResults) {
            try {
                String name = (String) obj.getClass().getField("name").get(obj);
                boolean passed = (boolean) obj.getClass().getField("passed").get(obj);
                String message = (String) obj.getClass().getField("message").get(obj);
                sb.append("<tr>");
                sb.append("<td>").append(escapeHtml(name)).append("</td>");
                sb.append("<td style='color:")
                        .append(passed ? "#28a745'>&#10004; Pass" : "#dc3545'>&#10008; Fail")
                        .append("</td>");
                sb.append("<td>").append(message == null ? "" : escapeHtml(message)).append("</td>");
                sb.append("</tr>");
            } catch (Exception ignore) {
            }
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
