package com.laker.postman.service.curl;

import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.PreparedRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * cURL 命令解析器，支持 Bash 和 CMD 格式
 */
@Slf4j
public class CurlParser {

    private CurlParser() {
        // 私有构造函数，防止实例化
    }

    /**
     * 解析 cURL 命令字符串为 CurlRequest 对象
     * 支持 Bash 和 Windows CMD 格式
     *
     * @param curl cURL 命令字符串
     * @return 解析后的 CurlRequest 对象
     */
    public static CurlRequest parse(String curl) {
        CurlRequest req = new CurlRequest();

        // Handle null or empty input
        if (curl == null || curl.trim().isEmpty()) {
            req.method = "GET"; // Set default method
            return req;
        }

        // 检测命令格式并进行预处理
        curl = preprocessCommand(curl);

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
            } else if (token.startsWith("http") || token.startsWith("ws://") || token.startsWith("wss://")) {
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
                    if (kv.length == 2) {
                        String headerName = kv[0].trim();
                        String headerValue = kv[1].trim();

                        // 如果是 WebSocket 请求，过滤掉 OkHttp 自动管理的头部
                        if (isWebSocketUrl(req.url) && isRestrictedWebSocketHeader(headerName)) {
                            // 跳过受限制的 WebSocket 头部，不添加到 headers 中
                            continue;
                        }

                        req.headers.put(headerName, headerValue);
                    }
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
                    // tokenize 方法已经处理了 shell 层面的转义，不需要再次 unescape
                    req.body = rawBody;
                    if (req.method == null) req.method = "POST"; // 有 body 默认 POST
                    String contentType = req.headers.getOrDefault("Content-Type", "");
                    // 如果是 multipart/form-data 格式，解析表单数据
                    if (contentType.startsWith("multipart/form-data")) {
                        parseMultipartFormData(req, req.body);
                    }
                }
            }
            // 6. Form (multipart/form-data)
            else if (token.equals("-F") || token.equals("--form")) {
                if (i + 1 < tokens.size()) {
                    String formField = tokens.get(++i);
                    int eqIdx = formField.indexOf('=');
                    if (eqIdx > 0) {
                        String key = formField.substring(0, eqIdx);
                        String value = formField.substring(eqIdx + 1);
                        if (value.startsWith("@")) {
                            // 文件上传
                            req.formFiles.put(key, value.substring(1));
                        } else {
                            // 普通表单字段
                            req.formData.put(key, value);
                        }
                    }
                    if (req.method == null) req.method = "POST";
                    // 设置 Content-Type
                    if (!req.headers.containsKey("Content-Type")) {
                        req.headers.put("Content-Type", "multipart/form-data");
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
            log.error("解析 multipart/form-data 出错", e);
        }
    }

    /**
     * 解析 cURL 命令行参数为 token 列表，支持引号包裹和普通参数
     * 支持 Bash 和 Windows CMD 格式
     *
     * @param cmd cURL 命令字符串
     * @return 参数 token 列表
     */
    private static List<String> tokenize(String cmd) {
        // 1. 处理 Bash 格式的反斜杠续行符
        cmd = cmd.replaceAll("\\\\[\\r\\n]+", " ");
        // 2. 处理 Windows CMD 格式的 ^ 续行符
        cmd = cmd.replaceAll("\\^[\\r\\n]+", " ");

        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inDollarQuote = false;

        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);

            // 处理转义字符（在双引号或 $'...' 中）
            if (c == '\\' && (inDoubleQuote || inDollarQuote)) {
                if (i + 1 < cmd.length()) {
                    char next = cmd.charAt(i + 1);
                    // 在 $'...' 或双引号中处理转义序列
                    if (next == '"') {
                        currentToken.append('"');
                        i++;
                        continue;
                    } else if (next == '\\') {
                        currentToken.append('\\');
                        i++;
                        continue;
                    } else if (next == 'n') {
                        currentToken.append('\n');
                        i++;
                        continue;
                    } else if (next == 't') {
                        currentToken.append('\t');
                        i++;
                        continue;
                    } else if (next == 'r') {
                        currentToken.append('\r');
                        i++;
                        continue;
                    } else if (next == 'b') {
                        currentToken.append('\b');
                        i++;
                        continue;
                    } else if (next == 'f') {
                        currentToken.append('\f');
                        i++;
                        continue;
                    } else if (next == '\'') {
                        currentToken.append('\'');
                        i++;
                        continue;
                    }
                }
                currentToken.append(c);
                continue;
            }

            if (c == '\'' && !inDoubleQuote && !inDollarQuote) {
                if (inSingleQuote) {
                    // 结束单引号
                    inSingleQuote = false;
                } else {
                    // 开始单引号
                    inSingleQuote = true;
                }
                continue;
            }

            if (c == '"' && !inSingleQuote && !inDollarQuote) {
                if (inDoubleQuote) {
                    // 结束双引号
                    inDoubleQuote = false;
                } else {
                    // 开始双引号
                    inDoubleQuote = true;
                }
                continue;
            }

            // 处理 $'...' 格式
            if (c == '$' && i + 1 < cmd.length() && cmd.charAt(i + 1) == '\'' && !inSingleQuote && !inDoubleQuote) {
                inDollarQuote = true;
                i++; // 跳过下一个单引号
                continue;
            }

            if (c == '\'' && inDollarQuote) {
                inDollarQuote = false;
                continue;
            }

            if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote && !inDollarQuote) {
                // 空白字符，且不在引号内
                if (!currentToken.isEmpty()) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                continue;
            }

            currentToken.append(c);
        }

        // 添加最后一个 token
        if (!currentToken.isEmpty()) {
            tokens.add(currentToken.toString());
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


    /**
     * 预处理 cURL 命令字符串，支持 Windows CMD 格式
     *
     * @param curl cURL 命令字符串
     * @return 预处理后的命令字符串
     */
    private static String preprocessCommand(String curl) {
        // 1. 处理行末的 ^ 续行符（Windows CMD 特有）
        curl = curl.replaceAll("\\s*\\^\\s*[\\r\\n]+", " ");
        // 2. 处理行末的 \ 续行符（Bash 特有）
        curl = curl.replaceAll("\\s*\\\\\\s*[\\r\\n]+", " ");
        // 3. 替换连续的空格为一个空格
        curl = curl.replaceAll("\\s+", " ");
        // 4. 去除首尾空格
        curl = curl.trim();
        return curl;
    }

    /**
     * 判断给定的 URL 是否为 WebSocket URL
     *
     * @param url 待判断的 URL
     * @return 如果是 WebSocket URL，返回 true；否则返回 false
     */
    private static boolean isWebSocketUrl(String url) {
        return url != null && (url.startsWith("ws://") || url.startsWith("wss://"));
    }

    /**
     * 判断给定的头部名称是否为受限制的 WebSocket 头部
     *
     * @param headerName 待判断的头部名称
     * @return 如果是受限制的头部，返回 true；否则返回 false
     */
    private static boolean isRestrictedWebSocketHeader(String headerName) {
        // 受限制的 WebSocket 头部列表
        List<String> restrictedHeaders = List.of(
                "Connection",
                "Host",
                "Upgrade",
                "Sec-WebSocket-Key",
                "Sec-WebSocket-Version",
                "Sec-WebSocket-Extensions"
        );
        return restrictedHeaders.contains(headerName);
    }
}
