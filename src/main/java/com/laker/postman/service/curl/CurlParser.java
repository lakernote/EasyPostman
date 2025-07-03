package com.laker.postman.service.curl;

import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.PreparedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * cURL 命令解析器，支持常见参数格式
 */
public class CurlParser {
    public static CurlRequest parse(String curl) {
        CurlRequest req = new CurlRequest();
        // 去除换行符 和多余空格
        List<String> tokens = tokenize(curl);

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            // 跳过curl
            if ("curl".equals(token)) continue;
            // 跳过引号
            if (("\"").equals(token)) continue;
            // 1. 自动跟随重定向
            if (token.equals("--location") || token.equals("-L")) {
                req.followRedirects = true;
                continue;
            }
            // 1. URL 处理
            if (token.equals("--url") && i + 1 < tokens.size()) {
                req.url = tokens.get(++i);
            } else if (token.startsWith("http")) {
                req.url = token;
            }

            // 2. 方法
            else if (token.equals("-X") || token.equals("--request")) {
                if (i + 1 < tokens.size()) req.method = tokens.get(++i).toUpperCase();
            }

            // 3. Header
            else if (token.equals("-H") || token.equals("--header")) {
                if (i + 1 < tokens.size()) {
                    String[] kv = tokens.get(++i).split(":", 2);
                    if (kv.length == 2) req.headers.put(kv[0].trim(), kv[1].trim());
                }
            }

            // 4. Cookie
            else if (token.equals("-b") || token.equals("--cookie")) {
                if (i + 1 < tokens.size()) req.headers.put("Cookie", tokens.get(++i));
            }

            // 5. Body
            else if (token.equals("-d") || token.equals("--data")
                    || token.equals("--data-raw") || token.equals("--data-binary")) {
                if (i + 1 < tokens.size()) {
                    String rawBody = tokens.get(++i);
                    // 处理 --data-raw 的转义字符，还原为正常 JSON
                    req.body = unescapeJava(rawBody);
                    if (req.method == null) req.method = "POST"; // 有 body 默认 POST
                    String contentType = req.headers.getOrDefault("Content-Type", "");
                    // 如果是 multipart/form-data 格式，解析表单数据
                    if (contentType.startsWith("multipart/form-data")) {
                        parseMultipartFormData(req, req.body);
                    }
                }
            }
        }
        if (req.method == null) req.method = "GET"; // 默认 GET

        // 解析 URL 查询参数到 params
        if (req.url != null && req.url.contains("?")) {
            String[] urlParts = req.url.split("\\?", 2);
            String query = urlParts[1];
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    req.params.put(kv[0], kv[1]);
                } else if (kv.length == 1) {
                    req.params.put(kv[0], "");
                }
            }
        }
        return req;
    }

    /**
     * 解析 multipart/form-data 格式的请求体
     *
     * @param req  CurlRequest 对象
     * @param data 请求体内容
     * @throws IllegalArgumentException 如果 Content-Type 缺失或无效
     */
    private static void parseMultipartFormData(CurlRequest req, String data) {
        try {
            // 从 Content-Type 提取 boundary
            String contentType = req.headers.get("Content-Type");
            if (contentType == null || !contentType.contains("boundary=")) {
                throw new IllegalArgumentException("Content-Type 缺失或无效");
            }
            String boundary = "--" + contentType.split("boundary=")[1];

            // 分割每个 part
            String[] parts = data.split(Pattern.quote(boundary));
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || part.equals("--")) continue; // 跳过空 part 和结束标志

                // 提取 Content-Disposition
                Matcher dispositionMatcher = Pattern.compile("Content-Disposition: form-data;\\s*(.+)").matcher(part);
                if (dispositionMatcher.find()) {
                    String disposition = dispositionMatcher.group(1);

                    // 提取字段名
                    Matcher nameMatcher = Pattern.compile("name=\"([^\"]+)\"").matcher(disposition);
                    if (nameMatcher.find()) {
                        String fieldName = nameMatcher.group(1);

                        // 提取文件名（如果有）
                        Matcher fileNameMatcher = Pattern.compile("filename=\"([^\"]+)\"").matcher(disposition);
                        if (fileNameMatcher.find()) {
                            String fileName = fileNameMatcher.group(1);
                            req.formFiles.put(fieldName, fileName); // 存储文件名
                        } else {
                            // 提取字段值
                            int valueStart = part.indexOf("\r\n\r\n") + 4;
                            String value = part.substring(valueStart).trim();
                            req.formData.put(fieldName, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析 multipart/form-data 出错: " + e.getMessage());
        }
    }

    /**
     * 解析 cURL 命令行参数为 token 列表，支持引号包裹和普通参数
     *
     * @param cmd cURL 命令字符串
     * @return 参数 token 列表
     */
    private static List<String> tokenize(String cmd) {
        // 1. 先去除所有行末的反斜杠 \ 和换行（cURL 换行续行符），将多行合并为一行
        cmd = cmd.replaceAll("\\\\[\\r\\n]+", " ");
        List<String> tokens = new ArrayList<>();
        // 正则说明：
        //  ('([^']*)')      匹配单引号包裹的内容
        //  "([^"]*)"      匹配双引号包裹的内容
        //  \$'([^']*)'     匹配 $'...' 形式的内容
        //  \S+             匹配连续的非空白字符
        Matcher m = Pattern.compile("('([^']*)'|\"([^\"]*)\"|\\$'([^']*)'|\\S+)").matcher(cmd);
        while (m.find()) {
            String t = m.group();
            // 处理 $'...' 格式（保留内部的引号，但去除外部的 $'...'）
            if (t.startsWith("$'") && t.endsWith("'")) {
                t = t.substring(2, t.length() - 1);
            }
            // 去除普通的首尾引号（单引号或双引号）
            else if ((t.startsWith("'") && t.endsWith("'")) || (t.startsWith("\"") && t.endsWith("\""))) {
                t = t.substring(1, t.length() - 1);
            }
            tokens.add(t);
        }
        return tokens;
    }

    /**
     * 将 PreparedRequest 转换为 cURL 命令字符串
     */
    public static String toCurl(PreparedRequest preparedRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("curl");
        // method
        if (preparedRequest.method != null && !"GET".equalsIgnoreCase(preparedRequest.method)) {
            sb.append(" -X ").append(preparedRequest.method.toUpperCase());
        }
        // url
        if (preparedRequest.url != null) {
            sb.append(" \"").append(preparedRequest.url).append("\"");
        }
        // headers
        if (preparedRequest.headers != null) {
            for (var entry : preparedRequest.headers.entrySet()) {
                sb.append(" -H \"").append(entry.getKey()).append(": ").append(entry.getValue()).append("\"");
            }
        }
        // body
        if (preparedRequest.body != null && !preparedRequest.body.isEmpty()) {
            sb.append(" --data ").append(escapeShellArg(preparedRequest.body));
        }
        // form-data
        if (preparedRequest.formData != null && !preparedRequest.formData.isEmpty()) {
            for (var entry : preparedRequest.formData.entrySet()) {
                sb.append(" -F ").append(escapeShellArg(entry.getKey() + "=" + entry.getValue()));
            }
        }
        // form-files
        if (preparedRequest.formFiles != null && !preparedRequest.formFiles.isEmpty()) {
            for (var entry : preparedRequest.formFiles.entrySet()) {
                sb.append(" -F ").append(escapeShellArg(entry.getKey() + "=@" + entry.getValue()));
            }
        }
        return sb.toString();
    }

    // shell参数转义
    private static String escapeShellArg(String s) {
        if (s == null) return "''";
        if (s.contains("'")) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        } else {
            return "'" + s + "'";
        }
    }

    // 工具方法：将 \n、\t 等转义字符还原为真实字符
    private static String unescapeJava(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '\'': sb.append('\''); i++; break;
                    case '"': sb.append('"'); i++; break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}