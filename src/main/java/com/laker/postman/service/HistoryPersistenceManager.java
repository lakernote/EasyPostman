package com.laker.postman.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.model.RequestHistoryItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.util.SystemUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 历史记录持久化管理器
 */
public class HistoryPersistenceManager {
    private static final String HISTORY_FILE = SystemUtil.getUserHomeEasyPostmanPath() + "request_history.json";

    private static HistoryPersistenceManager instance;
    private final List<RequestHistoryItem> historyItems = new CopyOnWriteArrayList<>();

    private HistoryPersistenceManager() {
        ensureHistoryDirExists();
        loadHistory();
    }

    public static synchronized HistoryPersistenceManager getInstance() {
        if (instance == null) {
            instance = new HistoryPersistenceManager();
        }
        return instance;
    }

    private void ensureHistoryDirExists() {
        try {
            Path historyDir = Paths.get(SystemUtil.getUserHomeEasyPostmanPath());
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create history directory: " + e.getMessage());
        }
    }

    /**
     * 添加历史记录
     */
    public void addHistory(PreparedRequest request, HttpResponse response) {
        RequestHistoryItem item = new RequestHistoryItem(request, response);
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
            System.err.println("Failed to save history: " + e.getMessage());
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
     * 加载历史记录
     */
    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            String jsonString = Files.readString(Paths.get(HISTORY_FILE), StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return;
            }

            JSONArray jsonArray = JSONUtil.parseArray(jsonString);
            historyItems.clear();

            for (int i = 0; i < jsonArray.size(); i++) {
                try {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    RequestHistoryItem item = convertFromJson(jsonItem);
                    historyItems.add(item);
                } catch (Exception e) {
                    // 忽略无法恢复的历史记录项
                    System.err.println("Failed to restore history item: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load history: " + e.getMessage());
            // 如果加载失败，删除损坏的文件
            boolean deleted = file.delete();
            if (!deleted) {
                System.err.println("Failed to delete corrupted history file: " + file.getPath());
            }
        } catch (Exception e) {
            System.err.println("Failed to parse history JSON: " + e.getMessage());
            // JSON 解析失败，删除损坏的文件
            boolean deleted = file.delete();
            if (!deleted) {
                System.err.println("Failed to delete corrupted history file: " + file.getPath());
            }
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
        jsonItem.set("timestamp", System.currentTimeMillis());

        // 请求信息
        JSONObject requestJson = new JSONObject();
        requestJson.set("method", item.request.method);
        requestJson.set("url", item.request.url);
        requestJson.set("body", item.request.body != null ? item.request.body : "");

        // 请求头
        JSONObject requestHeaders = new JSONObject();
        if (item.request.headers != null) {
            requestHeaders.putAll(item.request.headers);
        }
        requestJson.set("headers", requestHeaders);
        jsonItem.set("request", requestJson);

        // 响应信息
        JSONObject responseJson = new JSONObject();
        responseJson.set("code", item.response.code);
        responseJson.set("body", item.response.body != null ? item.response.body : "");
        responseJson.set("costMs", item.response.costMs);

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

        // 重建请求头
        request.headers = new java.util.HashMap<>();
        JSONObject requestHeaders = requestJson.getJSONObject("headers");
        if (requestHeaders != null) {
            for (String key : requestHeaders.keySet()) {
                request.headers.put(key, requestHeaders.getStr(key));
            }
        }

        // 重建 HttpResponse
        HttpResponse response = new HttpResponse();
        JSONObject responseJson = jsonItem.getJSONObject("response");
        response.code = responseJson.getInt("code");
        response.body = responseJson.getStr("body");
        response.costMs = responseJson.getLong("costMs");

        // 重建响应头
        response.headers = new java.util.HashMap<>();
        JSONObject responseHeaders = responseJson.getJSONObject("headers");
        if (responseHeaders != null) {
            for (String key : responseHeaders.keySet()) {
                String value = responseHeaders.getStr(key);
                java.util.List<String> valueList = new java.util.ArrayList<>();
                // 如果值包含逗号，分割为多个值
                String[] values = value.split(", ");
                for (String v : values) {
                    valueList.add(v.trim());
                }
                response.headers.put(key, valueList);
            }
        }

        return new RequestHistoryItem(request, response);
    }
}
