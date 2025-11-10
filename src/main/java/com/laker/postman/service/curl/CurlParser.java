package com.laker.postman.service.curl;

import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.PreparedRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * cURL 命令解析器，支持 Bash 格式
 *
 */
@Slf4j
public class CurlParser {

    private static final String CONTENT_TYPE = "Content-Type";

    // 受限制的 WebSocket 头部列表（静态常量，避免重复创建）
    private static final List<String> RESTRICTED_WEBSOCKET_HEADERS = List.of(
            "Connection",
            "Host",
            "Upgrade",
            "Sec-WebSocket-Key",
            "Sec-WebSocket-Version",
            "Sec-WebSocket-Extensions"
    );

    private CurlParser() {
        // 私有构造函数，防止实例化
    }

    /**
     * 解析 cURL 命令字符串为 CurlRequest 对象
     * 支持 Bash 格式
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

        // 预处理命令
        curl = preprocessCommand(curl);

        // 去除换行符和多余空格
        List<String> tokens = tokenize(curl);

        // 用于跟踪是否使用了 -G 选项
        boolean forceGet = false;
        // 临时存储 data 参数，以便在发现 -G 时转换为查询参数
        List<String> dataParams = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            // 跳过curl
            if ("curl".equals(token)) continue;
            // 跳过引号
            if (("\"").equals(token)) continue;
            // 处理 -G 或 --get 选项
            if (token.equals("-G") || token.equals("--get")) {
                forceGet = true;
                continue;
            }
            // 1. 自动跟随重定向
            if (token.equals("--location") || token.equals("-L")) {
                req.followRedirects = true;
                continue;
            }
            // 1. URL 处理 (使用最后一个 URL，符合 curl 实际行为)
            if (token.equals("--url") && i + 1 < tokens.size()) {
                req.url = tokens.get(++i);
            } else if (token.startsWith("http") || token.startsWith("ws://") || token.startsWith("wss://")) {
                req.url = token;
            }

            // 2. 方法
            else if (token.equals("-X") || token.equals("--request")) {
                if (i + 1 < tokens.size()) req.method = tokens.get(++i).toUpperCase();
            }
            // 支持 -XPOST, -XGET 等连写格式
            else if (token.startsWith("-X") && token.length() > 2) {
                req.method = token.substring(2).toUpperCase();
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
                    || token.equals("--data-raw") || token.equals("--data-binary")
                    || token.equals("--data-urlencode")) {
                if (i + 1 < tokens.size()) {
                    String rawBody = tokens.get(++i);
                    // 暂存到 dataParams，稍后根据 forceGet 决定如何处理
                    dataParams.add(rawBody);
                    if (req.method == null) req.method = "POST"; // 有 body 默认 POST
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
                    if (!req.headers.containsKey(CONTENT_TYPE)) {
                        req.headers.put(CONTENT_TYPE, "multipart/form-data");
                    }
                }
            }
        }

        // 处理 -G 选项：将 data 参数转换为 URL 查询参数
        if (forceGet) {
            req.method = "GET";
            // 将 dataParams 添加到 URL 查询参数
            for (String dataParam : dataParams) {
                String[] kv = dataParam.split("=", 2);
                if (kv.length == 2) {
                    req.params.put(kv[0], kv[1]);
                } else if (kv.length == 1) {
                    req.params.put(kv[0], "");
                }
            }
        } else {
            // 不是 -G 模式，将 dataParams 作为请求体
            if (!dataParams.isEmpty()) {
                // 如果没有设置 Content-Type，默认设置为 application/x-www-form-urlencoded
                if (!req.headers.containsKey(CONTENT_TYPE)) {
                    req.headers.put(CONTENT_TYPE, "application/x-www-form-urlencoded");
                }
                String contentType = req.headers.getOrDefault(CONTENT_TYPE, "");

                // 检查所有 dataParams 是否都是 key=value 格式
                boolean allKeyValue = true;
                for (String dataParam : dataParams) {
                    if (!dataParam.contains("=")) {
                        allKeyValue = false;
                        break;
                    }
                }

                // 如果是 application/x-www-form-urlencoded 格式且所有参数都是 key=value 格式，解析为 key-value 对
                if (contentType.startsWith("application/x-www-form-urlencoded") && allKeyValue) {
                    for (String dataParam : dataParams) {
                        String[] kv = dataParam.split("=", 2);
                        if (kv.length == 2) {
                            req.urlencoded.put(kv[0], kv[1]);
                        } else if (kv.length == 1) {
                            req.urlencoded.put(kv[0], "");
                        }
                    }
                }
                // 如果是 multipart/form-data 格式，解析表单数据
                else if (contentType.startsWith("multipart/form-data")) {
                    // 合并所有 data 参数为请求体
                    req.body = String.join("&", dataParams);
                    parseMultipartFormData(req, req.body);
                }
                // 其他情况（包括非 key=value 格式），保持为原始 body 字符串
                else {
                    req.body = String.join("&", dataParams);
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
            String contentType = req.headers.get(CONTENT_TYPE);
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
     * 支持 Bash 格式的单引号、双引号和 $'...' 格式
     *
     * @param cmd cURL 命令字符串
     * @return 参数 token 列表
     */
    private static List<String> tokenize(String cmd) {
        // 处理 Bash 格式的反斜杠续行符
        cmd = cmd.replaceAll("\\\\[\\r\\n]+", " ");

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
                    } else if (next == 'a') {
                        currentToken.append('\u0007'); // alert (bell)
                        i++;
                        continue;
                    } else if (next == 'v') {
                        currentToken.append('\u000B'); // vertical tab
                        i++;
                        continue;
                    } else if (next == '0') {
                        currentToken.append('\0'); // null character
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
     * 将 PreparedRequest 转换为 cURL 命令字符串（Bash 格式）
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
        // urlencoded (application/x-www-form-urlencoded)
        if (preparedRequest.urlencoded != null && !preparedRequest.urlencoded.isEmpty()) {
            for (var entry : preparedRequest.urlencoded.entrySet()) {
                sb.append(" --data-urlencode ").append(escapeShellArg(entry.getKey() + "=" + entry.getValue()));
            }
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

    // Bash shell参数转义
    private static String escapeShellArg(String s) {
        if (s == null) return "''";
        if (s.contains("'")) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        } else {
            return "'" + s + "'";
        }
    }


    /**
     * 预处理 cURL 命令字符串
     *
     * @param curl cURL 命令字符串
     * @return 预处理后的命令字符串
     */
    private static String preprocessCommand(String curl) {
        // 处理 Bash 行末的 \ 续行符
        curl = curl.replaceAll("\\s*\\\\\\s*[\\r\\n]+", " ");
        // 替换连续的空格为一个空格
        curl = curl.replaceAll("\\s+", " ");
        // 去除首尾空格
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
        return RESTRICTED_WEBSOCKET_HEADERS.contains(headerName);
    }
}
