package com.laker.postman.service;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.model.HttpRequestItem;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求服务类，负责发送 HTTP 请求并返回响应
 */
@Slf4j
public class HttpService {

    /**
     * 发送 HTTP 请求，支持环境变量替换
     *
     * @param urlString 请求地址
     * @param method    请求方法（GET、POST 等）
     * @param headers   请求头
     * @param body      请求体
     * @return HttpResponse 响应对象
     * @throws Exception 发送请求异常
     */
    public static HttpResponse sendRequest(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects) throws Exception {

        return doSendRequest(urlString, method, headers, body, followRedirects);
    }

    /**
     * 兼容老接口，默认自动重定向
     */
    public static HttpResponse sendRequest(String urlString, String method, Map<String, String> headers, String body) throws Exception {
        return sendRequest(urlString, method, headers, body, true);
    }

    /**
     * 处理请求头中的环境变量
     */
    public static Map<String, String> processHeaders(Map<String, String> headers) {
        if (headers == null) return null;

        Map<String, String> processedHeaders = new HashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 处理请求头的键和值中的环境变量
            String processedKey = EnvironmentService.replaceVariables(key);
            String processedValue = EnvironmentService.replaceVariables(value);

            processedHeaders.put(processedKey, processedValue);
        }

        return processedHeaders;
    }

    /**
     * 实际发送 HTTP 请求
     *
     * @param urlString 请求地址
     * @param method    请求方法（GET、POST 等）
     * @param headers   请求头
     * @param body      请求体
     * @return HttpResponse 响应对象
     * @throws Exception 发送请求异常
     */
    private static HttpResponse doSendRequest(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects) throws Exception {
        HttpURLConnection conn = null;
        try {
            // 创建 URL 对象
            URL url = new URL(urlString);
            // 打开连接
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(followRedirects);
            // 设置请求方法
            conn.setRequestMethod(method);
            // 设置连接超时时间
            conn.setConnectTimeout(5000);
            // 设置读取超时时间
            conn.setReadTimeout(5000);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 如果需要发送请求体
            if (body != null && !body.isEmpty() && !"GET".equalsIgnoreCase(method)) {
                // 设置为可输出
                conn.setDoOutput(true);
                // 写入请求体
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // 创建响应对象
            HttpResponse response = new HttpResponse();
            // 读取响应码
            int responseCode = conn.getResponseCode();
            response.code = responseCode; // 设置响应状态码
            // 读取响应头
            response.headers = conn.getHeaderFields();

            // 读取响应体
            InputStream inputStream;
            if (responseCode >= 400) {
                inputStream = conn.getErrorStream();
            } else {
                inputStream = conn.getInputStream();
            }

            // 没有响应体则返回空内容
            if (inputStream == null) {
                response.body = "";
                return response;
            }

            // 读取响应内容
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder responseContent = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseContent.append(line).append("\n");
                }
                response.body = responseContent.toString();
            }

            return response;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 发送 multipart/form-data 请求，支持文本字段和文件字段
     */
    public static HttpResponse sendRequestWithMultipart(String urlString, String method, Map<String, String> headers,
                                                        Map<String, String> formData, Map<String, String> formFiles, boolean followRedirects) throws Exception {
        String boundary = "----EasyToolsBoundary" + System.currentTimeMillis();
        String contentType = "multipart/form-data; boundary=" + boundary;
        if (headers == null) headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(followRedirects);
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            // 设置请求头
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            try (OutputStream os = conn.getOutputStream()) {
                // 文本字段
                if (formData != null) {
                    for (Map.Entry<String, String> entry : formData.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                        os.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        os.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
                        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
                // 文件字段
                if (formFiles != null) {
                    for (Map.Entry<String, String> entry : formFiles.entrySet()) {
                        String key = entry.getKey();
                        String filePath = entry.getValue();
                        File file = new File(filePath);
                        if (!file.exists()) continue;
                        String fileName = file.getName();
                        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                        os.write(("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                        os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        try (InputStream fis = new java.io.FileInputStream(file)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = fis.read(buf)) != -1) {
                                os.write(buf, 0, len);
                            }
                        }
                        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
                // 结束 boundary
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            // 读取响应
            HttpResponse response = new HttpResponse();
            int responseCode = conn.getResponseCode();
            response.code = responseCode;
            response.headers = conn.getHeaderFields();
            InputStream inputStream = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (inputStream == null) {
                response.body = "";
                return response;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder responseContent = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseContent.append(line).append("\n");
                }
                response.body = responseContent.toString();
            }
            return response;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 兼容老接口，默认自动重定向
     */
    public static HttpResponse sendRequestWithMultipart(String urlString, String method, Map<String, String> headers,
                                                        Map<String, String> formData, Map<String, String> formFiles) throws Exception {
        return sendRequestWithMultipart(urlString, method, headers, formData, formFiles, true);
    }

    public static HttpRequestItem createDefaultRequest() {
        // 创建一个测试请求
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("测试请求");
        testItem.setUrl("https://httpbin.org/get");
        testItem.setMethod("GET");

        // 添加一些默认的请求头
        testItem.getHeaders().put("User-Agent", "EasyTools HTTP Client");
        testItem.getHeaders().put("Accept", "*/*");
        return testItem;
    }

    /**
     * HTTP 响应类
     */
    public static class HttpResponse {
        public Map<String, List<String>> headers;
        public String body;
        public int code; // 添加响应状态码字段
        public String threadName; // 添加线程名称字段
    }
}