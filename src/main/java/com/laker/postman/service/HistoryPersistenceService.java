package com.laker.postman.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.ioc.Component;
import com.laker.postman.ioc.PostConstruct;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 历史记录持久化管理器
 */
@Slf4j
@Component
public class HistoryPersistenceService {
    private static final String HISTORY_FILE = ConfigPathConstants.REQUEST_HISTORY;

    // 限制单个响应体保存的最大字符数 (10KB)
    private static final int MAX_BODY_SIZE = 10 * 1024;
    // 限制单个请求体保存的最大字符数 (10KB)
    private static final int MAX_REQUEST_BODY_SIZE = 10 * 1024;
    // 限制文件大小 (50MB)
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final List<RequestHistoryItem> historyItems = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        ensureHistoryDirExists();
        loadHistory();
    }

    private void ensureHistoryDirExists() {
        try {
            Path historyDir = Paths.get(SystemUtil.getEasyPostmanPath());
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }
        } catch (IOException e) {
            log.error("Failed to create history directory: {}", e.getMessage());
        }
    }

    /**
     * 添加历史记录
     */
    public void addHistory(PreparedRequest request, HttpResponse response, long requestTime) {
        RequestHistoryItem item = new RequestHistoryItem(request, response, requestTime);
        historyItems.add(0, item); // 添加到开头

        // 限制历史记录数量
        int maxCount = SettingManager.getMaxHistoryCount();
        while (historyItems.size() > maxCount) {
            historyItems.remove(historyItems.size() - 1);
        }

        // 异步保存
        saveHistoryAsync();
    }

    /**
     * 获取所有历史记录
     */
    public List<RequestHistoryItem> getHistory() {
        return new ArrayList<>(historyItems);
    }

    /**
     * 清空历史记录
     */
    public void clearHistory() {
        historyItems.clear();
        saveHistoryAsync();
    }

    /**
     * 同步保存历史记录
     */
    public void saveHistory() {
        try {
            // 只保存有限的历史记录
            int maxCount = SettingManager.getMaxHistoryCount();
            JSONArray jsonArray = new JSONArray();

            int count = Math.min(historyItems.size(), maxCount);
            for (int i = 0; i < count; i++) {
                RequestHistoryItem item = historyItems.get(i);
                JSONObject jsonItem = convertToJson(item);
                jsonArray.add(jsonItem);
            }

            // 写入文件
            String jsonString = JSONUtil.toJsonPrettyStr(jsonArray);
            Files.writeString(Paths.get(HISTORY_FILE), jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to save history: {}", e.getMessage());
        }
    }

    /**
     * 异步保存历史记录
     */
    private void saveHistoryAsync() {
        Thread saveThread = new Thread(this::saveHistory);
        saveThread.setDaemon(true);
        saveThread.start();
    }

    /**
     * 截断过大的body内容
     */
    private String truncateBody(String body, int maxSize) {
        if (body == null) {
            return "";
        }
        if (body.length() <= maxSize) {
            return body;
        }
        // 截断并添加提示信息
        return body.substring(0, maxSize) + "\n\n... [内容过大，已截断。原始大小: " + body.length() + " 字符] ...";
    }

    /**
     * 加载历史记录 - 使用流式读取避免内存溢出
     */
    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            // 检查文件大小，如果超过限制则删除文件并重新开始
            long fileSizeInBytes = file.length();

            if (fileSizeInBytes > MAX_FILE_SIZE) {
                log.warn("History file is too large ({} bytes, max: {} bytes), deleting and starting fresh",
                        fileSizeInBytes, MAX_FILE_SIZE);
                deleteHistoryFile(file);
                return;
            }

            // 如果文件为空，直接返回
            if (fileSizeInBytes == 0) {
                return;
            }

            // 使用流式读取，避免一次性加载整个文件到内存
            String jsonString = loadFileContent(file);
            if (jsonString.trim().isEmpty()) {
                return;
            }

            JSONArray jsonArray = JSONUtil.parseArray(jsonString);
            historyItems.clear();

            // 限制加载的历史记录数量
            int maxCount = SettingManager.getMaxHistoryCount();
            int loadCount = Math.min(jsonArray.size(), maxCount);

            for (int i = 0; i < loadCount; i++) {
                try {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    RequestHistoryItem item = convertFromJson(jsonItem);
                    historyItems.add(item);
                } catch (Exception e) {
                    // 忽略无法恢复的历史记录项
                    log.warn("Failed to restore history item at index {}: {}", i, e.getMessage());
                }
            }

            log.info("Successfully loaded {} history items from file", historyItems.size());

        } catch (OutOfMemoryError e) {
            log.error("Out of memory while loading history file, deleting and starting fresh", e);
            // 内存溢出，删除文件并重新开始
            deleteHistoryFile(file);
            historyItems.clear();
        } catch (Exception e) {
            log.error("Failed to load history: {}", e.getMessage(), e);
            // 加载失败，删除损坏的文件
            deleteHistoryFile(file);
            historyItems.clear();
        }
    }

    /**
     * 使用流式方式读取文件内容，更节省内存
     */
    private String loadFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder((int) Math.min(file.length(), MAX_FILE_SIZE));
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            char[] buffer = new char[8192]; // 8KB buffer
            int charsRead;
            long totalCharsRead = 0;
            long maxChars = MAX_FILE_SIZE / 2; // 限制读取字符数，防止超大文件

            while ((charsRead = reader.read(buffer)) != -1) {
                totalCharsRead += charsRead;
                if (totalCharsRead > maxChars) {
                    log.warn("History file content too large, truncating at {} chars", totalCharsRead);
                    break;
                }
                content.append(buffer, 0, charsRead);
            }
        }
        return content.toString();
    }

    /**
     * 删除历史文件
     */
    private void deleteHistoryFile(File file) {
        try {
            Files.delete(file.toPath());
            log.info("Deleted history file: {}", file.getPath());
        } catch (IOException e) {
            log.error("Failed to delete history file: {}", file.getPath(), e);
        }
    }

    /**
     * 将 RequestHistoryItem 转换为 JSON 对象
     */
    private JSONObject convertToJson(RequestHistoryItem item) {
        JSONObject jsonItem = new JSONObject();

        // 基本信息
        jsonItem.set("method", item.method);
        jsonItem.set("url", item.url);
        jsonItem.set("responseCode", item.responseCode);
        jsonItem.set("requestTime", item.requestTime); // 新增请求时间

        // 请求信息
        JSONObject requestJson = new JSONObject();
        requestJson.set("method", item.request.method);
        requestJson.set("url", item.request.url);
        // 请求体 - 优先保存实际发送的okHttpRequestBody，但限制大小
        String requestBody = "";
        if (item.request.okHttpRequestBody != null && !item.request.okHttpRequestBody.isEmpty()) {
            requestBody = item.request.okHttpRequestBody;
        } else if (item.request.body != null) {
            requestBody = item.request.body;
        }
        // 限制请求体大小
        requestBody = truncateBody(requestBody, MAX_REQUEST_BODY_SIZE);
        requestJson.set("body", requestBody);
        requestJson.set("id", item.request.id);
        requestJson.set("followRedirects", item.request.followRedirects);
        requestJson.set("logEvent", item.request.logEvent);
        requestJson.set("isMultipart", item.request.isMultipart);

        // 请求头 - 优先保存实际发送的okHttpHeaders
        JSONObject requestHeaders = new JSONObject();
        if (item.request.okHttpHeaders != null && item.request.okHttpHeaders.size() > 0) {
            // 使用实际发送的OkHttp Headers
            for (int i = 0; i < item.request.okHttpHeaders.size(); i++) {
                String name = item.request.okHttpHeaders.name(i);
                String value = item.request.okHttpHeaders.value(i);
                requestHeaders.set(name, value);
            }
        }
        requestJson.set("headers", requestHeaders);


        jsonItem.set("request", requestJson);

        // 响应信息
        JSONObject responseJson = new JSONObject();
        responseJson.set("code", item.response.code);
        // 限制响应体大小
        String responseBody = item.response.body != null ? item.response.body : "";
        responseBody = truncateBody(responseBody, MAX_BODY_SIZE);
        responseJson.set("body", responseBody);
        responseJson.set("costMs", item.response.costMs);
        responseJson.set("threadName", item.response.threadName);
        responseJson.set("filePath", item.response.filePath);
        responseJson.set("fileName", item.response.fileName);
        responseJson.set("protocol", item.response.protocol);
        responseJson.set("idleConnectionCount", item.response.idleConnectionCount);
        responseJson.set("connectionCount", item.response.connectionCount);
        responseJson.set("bodySize", item.response.bodySize);
        responseJson.set("headersSize", item.response.headersSize);
        responseJson.set("isSse", item.response.isSse);

        // 响应头
        JSONObject responseHeaders = new JSONObject();
        if (item.response.headers != null) {
            for (java.util.Map.Entry<String, java.util.List<String>> entry : item.response.headers.entrySet()) {
                String key = entry.getKey();
                java.util.List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    responseHeaders.set(key, String.join(", ", values));
                }
            }
        }
        responseJson.set("headers", responseHeaders);

        // 事件信息
        if (item.response.httpEventInfo != null) {
            JSONObject eventInfo = new JSONObject();
            eventInfo.set("localAddress", item.response.httpEventInfo.getLocalAddress());
            eventInfo.set("remoteAddress", item.response.httpEventInfo.getRemoteAddress());
            eventInfo.set("queueStart", item.response.httpEventInfo.getQueueStart());
            eventInfo.set("callStart", item.response.httpEventInfo.getCallStart());
            eventInfo.set("proxySelectStart", item.response.httpEventInfo.getProxySelectStart());
            eventInfo.set("proxySelectEnd", item.response.httpEventInfo.getProxySelectEnd());
            eventInfo.set("dnsStart", item.response.httpEventInfo.getDnsStart());
            eventInfo.set("dnsEnd", item.response.httpEventInfo.getDnsEnd());
            eventInfo.set("connectStart", item.response.httpEventInfo.getConnectStart());
            eventInfo.set("secureConnectStart", item.response.httpEventInfo.getSecureConnectStart());
            eventInfo.set("secureConnectEnd", item.response.httpEventInfo.getSecureConnectEnd());
            eventInfo.set("connectEnd", item.response.httpEventInfo.getConnectEnd());
            eventInfo.set("connectionAcquired", item.response.httpEventInfo.getConnectionAcquired());
            eventInfo.set("requestHeadersStart", item.response.httpEventInfo.getRequestHeadersStart());
            eventInfo.set("requestHeadersEnd", item.response.httpEventInfo.getRequestHeadersEnd());
            eventInfo.set("requestBodyStart", item.response.httpEventInfo.getRequestBodyStart());
            eventInfo.set("requestBodyEnd", item.response.httpEventInfo.getRequestBodyEnd());
            eventInfo.set("responseHeadersStart", item.response.httpEventInfo.getResponseHeadersStart());
            eventInfo.set("responseHeadersEnd", item.response.httpEventInfo.getResponseHeadersEnd());
            eventInfo.set("responseBodyStart", item.response.httpEventInfo.getResponseBodyStart());
            eventInfo.set("responseBodyEnd", item.response.httpEventInfo.getResponseBodyEnd());
            eventInfo.set("connectionReleased", item.response.httpEventInfo.getConnectionReleased());
            eventInfo.set("callEnd", item.response.httpEventInfo.getCallEnd());
            eventInfo.set("callFailed", item.response.httpEventInfo.getCallFailed());
            eventInfo.set("canceled", item.response.httpEventInfo.getCanceled());
            eventInfo.set("queueingCost", item.response.httpEventInfo.getQueueingCost());
            eventInfo.set("stalledCost", item.response.httpEventInfo.getStalledCost());
            eventInfo.set("protocol", item.response.httpEventInfo.getProtocol() != null ? item.response.httpEventInfo.getProtocol().toString() : null);
            eventInfo.set("tlsVersion", item.response.httpEventInfo.getTlsVersion());
            eventInfo.set("errorMessage", item.response.httpEventInfo.getErrorMessage());
            eventInfo.set("threadName", item.response.httpEventInfo.getThreadName());
            responseJson.set("httpEventInfo", eventInfo);
        }

        jsonItem.set("response", responseJson);

        return jsonItem;
    }

    /**
     * 从 JSON 对象转换为 RequestHistoryItem
     */
    private RequestHistoryItem convertFromJson(JSONObject jsonItem) {
        // 重建 PreparedRequest
        PreparedRequest request = new PreparedRequest();
        JSONObject requestJson = jsonItem.getJSONObject("request");
        request.method = requestJson.getStr("method");
        request.url = requestJson.getStr("url");
        request.body = requestJson.getStr("body");
        // 同时设置 okHttpRequestBody 供 HttpHtmlRenderer 使用
        request.okHttpRequestBody = request.body;
        request.id = requestJson.getStr("id");
        request.followRedirects = requestJson.getBool("followRedirects", true);
        request.logEvent = requestJson.getBool("logEvent", false);
        request.isMultipart = requestJson.getBool("isMultipart", false);

        // 重建请求头 - 构建 okHttpHeaders 供 HttpHtmlRenderer 使用
        JSONObject requestHeaders = requestJson.getJSONObject("headers");
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
            for (String key : requestHeaders.keySet()) {
                try {
                    headersBuilder.add(key, requestHeaders.getStr(key));
                } catch (Exception e) {
                    // 忽略无效的头信息
                }
            }
            request.okHttpHeaders = headersBuilder.build();
        }

        // 重建 HttpResponse
        HttpResponse response = new HttpResponse();
        JSONObject responseJson = jsonItem.getJSONObject("response");
        response.code = responseJson.getInt("code");
        response.body = responseJson.getStr("body");
        response.costMs = responseJson.getLong("costMs", 0L);
        response.threadName = responseJson.getStr("threadName");
        response.filePath = responseJson.getStr("filePath");
        response.fileName = responseJson.getStr("fileName");
        response.protocol = responseJson.getStr("protocol");
        response.idleConnectionCount = responseJson.getInt("idleConnectionCount", 0);
        response.connectionCount = responseJson.getInt("connectionCount", 0);
        response.bodySize = responseJson.getInt("bodySize", 0);
        response.headersSize = responseJson.getInt("headersSize", 0);
        response.isSse = responseJson.getBool("isSse", false);

        // 重建响应头
        response.headers = new LinkedHashMap<>();
        JSONObject responseHeaders = responseJson.getJSONObject("headers");
        if (responseHeaders != null) {
            for (String key : responseHeaders.keySet()) {
                String value = responseHeaders.getStr(key);
                List<String> valueList = new ArrayList<>();
                // 如果值包含逗号，分割为多个值
                String[] values = value.split(", ");
                for (String v : values) {
                    valueList.add(v.trim());
                }
                response.headers.put(key, valueList);
            }
        }

        // 重建事件信息
        JSONObject eventInfoJson = responseJson.getJSONObject("httpEventInfo");
        if (eventInfoJson != null) {
            response.httpEventInfo = new com.laker.postman.model.HttpEventInfo();
            response.httpEventInfo.setLocalAddress(eventInfoJson.getStr("localAddress"));
            response.httpEventInfo.setRemoteAddress(eventInfoJson.getStr("remoteAddress"));
            response.httpEventInfo.setQueueStart(eventInfoJson.getLong("queueStart", 0L));
            response.httpEventInfo.setCallStart(eventInfoJson.getLong("callStart", 0L));
            response.httpEventInfo.setProxySelectStart(eventInfoJson.getLong("proxySelectStart", 0L));
            response.httpEventInfo.setProxySelectEnd(eventInfoJson.getLong("proxySelectEnd", 0L));
            response.httpEventInfo.setDnsStart(eventInfoJson.getLong("dnsStart", 0L));
            response.httpEventInfo.setDnsEnd(eventInfoJson.getLong("dnsEnd", 0L));
            response.httpEventInfo.setConnectStart(eventInfoJson.getLong("connectStart", 0L));
            response.httpEventInfo.setSecureConnectStart(eventInfoJson.getLong("secureConnectStart", 0L));
            response.httpEventInfo.setSecureConnectEnd(eventInfoJson.getLong("secureConnectEnd", 0L));
            response.httpEventInfo.setConnectEnd(eventInfoJson.getLong("connectEnd", 0L));
            response.httpEventInfo.setConnectionAcquired(eventInfoJson.getLong("connectionAcquired", 0L));
            response.httpEventInfo.setRequestHeadersStart(eventInfoJson.getLong("requestHeadersStart", 0L));
            response.httpEventInfo.setRequestHeadersEnd(eventInfoJson.getLong("requestHeadersEnd", 0L));
            response.httpEventInfo.setRequestBodyStart(eventInfoJson.getLong("requestBodyStart", 0L));
            response.httpEventInfo.setRequestBodyEnd(eventInfoJson.getLong("requestBodyEnd", 0L));
            response.httpEventInfo.setResponseHeadersStart(eventInfoJson.getLong("responseHeadersStart", 0L));
            response.httpEventInfo.setResponseHeadersEnd(eventInfoJson.getLong("responseHeadersEnd", 0L));
            response.httpEventInfo.setResponseBodyStart(eventInfoJson.getLong("responseBodyStart", 0L));
            response.httpEventInfo.setResponseBodyEnd(eventInfoJson.getLong("responseBodyEnd", 0L));
            response.httpEventInfo.setConnectionReleased(eventInfoJson.getLong("connectionReleased", 0L));
            response.httpEventInfo.setCallEnd(eventInfoJson.getLong("callEnd", 0L));
            response.httpEventInfo.setCallFailed(eventInfoJson.getLong("callFailed", 0L));
            response.httpEventInfo.setCanceled(eventInfoJson.getLong("canceled", 0L));
            response.httpEventInfo.setQueueingCost(eventInfoJson.getLong("queueingCost", 0L));
            response.httpEventInfo.setStalledCost(eventInfoJson.getLong("stalledCost", 0L));

            // 处理协议类型
            String protocolStr = eventInfoJson.getStr("protocol");
            if (protocolStr != null && !protocolStr.isEmpty()) {
                try {
                    response.httpEventInfo.setProtocol(okhttp3.Protocol.valueOf(protocolStr));
                } catch (IllegalArgumentException e) {
                    // 忽略无法解析的协议类型
                }
            }

            response.httpEventInfo.setTlsVersion(eventInfoJson.getStr("tlsVersion"));
            response.httpEventInfo.setErrorMessage(eventInfoJson.getStr("errorMessage"));
            response.httpEventInfo.setThreadName(eventInfoJson.getStr("threadName"));
        }

        // 读取请求时间
        long requestTime = jsonItem.getLong("requestTime", System.currentTimeMillis());

        return new RequestHistoryItem(request, response, requestTime);
    }
}
