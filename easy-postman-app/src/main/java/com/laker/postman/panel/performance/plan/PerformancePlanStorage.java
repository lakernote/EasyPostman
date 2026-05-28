package com.laker.postman.panel.performance.plan;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanJsonStorage;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.request.PerformanceRequestExecutionScopeSnapshot;
import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.request.PerformanceRequestKeyValue;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class PerformancePlanStorage {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private final PerformanceCorePlanJsonStorage corePlanJsonStorage = new PerformanceCorePlanJsonStorage();

    public void saveConfiguration(Path configPath, PerformancePlanConfiguration configuration) {
        if (configPath == null) {
            return;
        }
        try {
            PerformancePlanConfiguration safeConfiguration = configuration == null
                    ? PerformancePlanConfiguration.builder().build()
                    : configuration;
            ensureDirExists(configPath);
            JSONObject jsonRoot = new JSONObject();
            jsonRoot.set("version", "1.0");
            jsonRoot.set("efficientMode", safeConfiguration.isEfficientMode());
            jsonRoot.set("trendEnabled", safeConfiguration.isTrendEnabled());
            jsonRoot.set("reportRealtimeEnabled", safeConfiguration.isReportRealtimeEnabled());
            PerformancePlanDocument document = safeConfiguration.getPlanDocument();
            PerformanceCorePlanDocument coreDocument = PerformanceCorePlanAdapter.toCoreDocument(document);
            jsonRoot.set("tree", corePlanJsonStorage.toTreeMap(coreDocument == null ? null : coreDocument.getRoot()));
            if (safeConfiguration.getCsvState() != null) {
                jsonRoot.set("csvState", serializeCsvState(safeConfiguration.getCsvState()));
            }

            Files.writeString(configPath, JSONUtil.toJsonPrettyStr(jsonRoot), StandardCharsets.UTF_8);
            log.info("Successfully saved performance test configuration (efficientMode: {}, trendEnabled: {}, reportRealtimeEnabled: {})",
                    safeConfiguration.isEfficientMode(),
                    safeConfiguration.isTrendEnabled(),
                    safeConfiguration.isReportRealtimeEnabled());
        } catch (IOException e) {
            log.error("Failed to save performance test config: {}", e.getMessage(), e);
        }
    }

    public PerformancePlanConfiguration loadConfiguration(Path configPath,
                                                          Function<String, HttpRequestItem> requestResolver) {
        if (configPath == null) {
            return null;
        }
        File file = configPath.toFile();
        if (!file.exists()) {
            log.info("No performance test config file found, starting fresh");
            return null;
        }

        try {
            long fileSizeInBytes = file.length();
            if (fileSizeInBytes > MAX_FILE_SIZE) {
                log.warn("Config file is too large ({} bytes), deleting and starting fresh", fileSizeInBytes);
                deleteFile(file);
                return null;
            }
            if (fileSizeInBytes == 0) {
                return null;
            }

            String jsonString = Files.readString(configPath, StandardCharsets.UTF_8);
            if (jsonString.trim().isEmpty()) {
                return null;
            }

            JSONObject jsonRoot = JSONUtil.parseObj(jsonString);
            log.info("Successfully loaded performance test configuration");
            return deserializeConfiguration(jsonRoot, requestResolver);
        } catch (Exception e) {
            log.error("Failed to load performance test config: {}", e.getMessage(), e);
            deleteFile(file);
            return null;
        }
    }

    public void clear(Path configPath) {
        if (configPath == null) {
            return;
        }
        File file = configPath.toFile();
        if (file.exists()) {
            deleteFile(file);
        }
    }

    private void ensureDirExists(Path configPath) throws IOException {
        Path configDir = configPath.getParent();
        if (configDir != null && !Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    private JSONObject serializeCsvState(PerformanceCsvState csvState) {
        JSONObject json = new JSONObject();
        json.set("sourceName", csvState.getSourceName());
        json.set("headers", new JSONArray(csvState.getHeaders()));

        JSONArray rows = new JSONArray();
        for (Map<String, String> row : csvState.getRows()) {
            JSONObject rowJson = new JSONObject();
            if (row != null) {
                row.forEach(rowJson::set);
            }
            rows.add(rowJson);
        }
        json.set("rows", rows);
        return json;
    }

    private PerformancePlanConfiguration deserializeConfiguration(JSONObject jsonRoot,
                                                                  Function<String, HttpRequestItem> requestResolver) {
        PerformancePlanDocument document = null;
        JSONObject treeJson = jsonRoot.getJSONObject("tree");
        if (treeJson != null) {
            if (canLoadWithCoreStorage(treeJson)) {
                PerformanceCorePlanNode coreRootNode = corePlanJsonStorage.fromTreeMap(jsonObjectToMap(treeJson));
                document = PerformanceCorePlanAdapter.toAppDocument(new PerformanceCorePlanDocument(coreRootNode));
            } else {
                PerformancePlanNode rootNode = deserializePlanNode(treeJson, requestResolver);
                if (rootNode != null) {
                    document = new PerformancePlanDocument(rootNode);
                }
            }
        }

        return PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(jsonRoot.getBool("efficientMode", true))
                .trendEnabled(jsonRoot.getBool("trendEnabled", true))
                .reportRealtimeEnabled(jsonRoot.getBool("reportRealtimeEnabled", false))
                .csvState(deserializeCsvState(jsonRoot.getJSONObject("csvState")))
                .build();
    }

    private boolean canLoadWithCoreStorage(JSONObject treeJson) {
        if (treeJson == null || treeJson.isEmpty()) {
            return false;
        }
        return !containsLegacyRequestFields(treeJson);
    }

    private boolean containsLegacyRequestFields(JSONObject nodeJson) {
        if (nodeJson == null || nodeJson.isEmpty()) {
            return false;
        }
        if (nodeJson.containsKey("requestItem") || nodeJson.containsKey("requestExecutionScope")) {
            return true;
        }
        if ("REQUEST".equals(nodeJson.getStr("type"))
                && nodeJson.containsKey("requestItemId")
                && !nodeJson.containsKey("requestSnapshot")) {
            return true;
        }
        JSONArray children = nodeJson.getJSONArray("children");
        if (children == null) {
            return false;
        }
        for (int i = 0; i < children.size(); i++) {
            if (containsLegacyRequestFields(children.getJSONObject(i))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonObjectToMap(JSONObject json) {
        return JsonUtil.convertValue(JsonUtil.readTree(json.toString()), Map.class);
    }

    private PerformancePlanNode deserializePlanNode(JSONObject jsonNode,
                                                    Function<String, HttpRequestItem> requestResolver) {
        try {
            String name = jsonNode.getStr("name");
            String typeStr = jsonNode.getStr("type");
            Boolean enabled = jsonNode.getBool("enabled", true);

            if (name == null || typeStr == null) {
                return null;
            }

            NodeType nodeType = NodeType.valueOf(typeStr);
            var builder = PerformancePlanNode.builder()
                    .name(name)
                    .type(nodeType)
                    .enabled(enabled);

            switch (nodeType) {
                case THREAD_GROUP -> {
                    JSONObject tgData = jsonNode.getJSONObject("threadGroupData");
                    if (tgData != null) {
                        builder.threadGroupData(deserializeThreadGroupData(tgData));
                    }
                }
                case CSV_DATA_SET -> {
                    JSONObject csvData = jsonNode.getJSONObject("csvDataSetData");
                    if (csvData != null) {
                        builder.csvDataSetData(deserializeCsvDataSetData(csvData));
                    }
                }
                case LOOP -> {
                    JSONObject loopData = jsonNode.getJSONObject("loopData");
                    builder.loopData(loopData == null ? new LoopData() : deserializeLoopData(loopData));
                }
                case REQUEST -> {
                    String requestItemId = jsonNode.getStr("requestItemId");
                    PerformanceRequestSnapshot requestSnapshot = deserializeRequestSnapshot(jsonNode.getJSONObject("requestSnapshot"));
                    HttpRequestItem requestItem = deserializeRequestItem(jsonNode.getJSONObject("requestItem"));
                    if (requestItem == null && requestItemId != null && requestResolver != null) {
                        requestItem = requestResolver.apply(requestItemId);
                        if (requestItem == null && requestSnapshot == null) {
                            log.warn("Request with ID {} not found in resolver, skipping", requestItemId);
                            return null;
                        }
                    }
                    if (requestItem == null && requestSnapshot != null) {
                        requestItem = PerformanceRequestSnapshotMapper.toHttpRequestItem(requestSnapshot);
                    }
                    if (requestItem != null) {
                        builder.httpRequestItem(requestItem);
                    }
                    if (requestSnapshot != null) {
                        builder.requestSnapshot(requestSnapshot);
                    }
                    builder.requestInheritanceSnapshot(jsonNode.getBool("requestInheritanceSnapshot", false));
                    JSONObject requestExecutionScope = jsonNode.getJSONObject("requestExecutionScope");
                    if (requestExecutionScope != null && !requestExecutionScope.isEmpty()) {
                        Map<String, String> groupVariables = new LinkedHashMap<>();
                        for (String key : requestExecutionScope.keySet()) {
                            groupVariables.put(key, requestExecutionScope.getStr(key));
                        }
                        builder.requestExecutionScope(RequestExecutionScope.fromGroupVariables(groupVariables));
                    } else if (requestSnapshot != null) {
                        builder.requestExecutionScope(PerformanceRequestSnapshotMapper.toRequestExecutionScope(requestSnapshot));
                    }
                    JSONObject wsData = jsonNode.getJSONObject("webSocketPerformanceData");
                    if (wsData != null) {
                        builder.webSocketPerformanceData(deserializeWebSocketPerformanceData(wsData));
                    }
                }
                case ASSERTION -> {
                    JSONObject assertionData = jsonNode.getJSONObject("assertionData");
                    if (assertionData != null) {
                        builder.assertionData(deserializeAssertionData(assertionData));
                    }
                }
                case EXTRACTOR -> {
                    JSONObject extractorData = jsonNode.getJSONObject("extractorData");
                    if (extractorData != null) {
                        builder.extractorData(deserializeExtractorData(extractorData));
                    }
                }
                case TIMER -> {
                    JSONObject timerData = jsonNode.getJSONObject("timerData");
                    if (timerData != null) {
                        builder.timerData(deserializeTimerData(timerData));
                    }
                }
                case WS_CONNECT, WS_SEND, WS_READ, WS_CLOSE -> {
                    JSONObject wsData = jsonNode.getJSONObject("webSocketPerformanceData");
                    if (wsData != null) {
                        builder.webSocketPerformanceData(deserializeWebSocketPerformanceData(wsData));
                    }
                }
                case SSE_CONNECT, SSE_READ -> {
                    JSONObject sseData = jsonNode.getJSONObject("ssePerformanceData");
                    if (sseData != null) {
                        builder.ssePerformanceData(deserializeSsePerformanceData(sseData));
                    }
                }
                case ROOT -> {
                }
            }

            List<PerformancePlanNode> childNodes = new ArrayList<>();
            JSONArray children = jsonNode.getJSONArray("children");
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    PerformancePlanNode childNode = deserializePlanNode(children.getJSONObject(i), requestResolver);
                    if (childNode != null) {
                        childNodes.add(childNode);
                    }
                }
            }

            return builder.children(childNodes).build();
        } catch (Exception e) {
            log.warn("Failed to deserialize performance plan node: {}", e.getMessage());
            return null;
        }
    }

    private HttpRequestItem deserializeRequestItem(JSONObject requestJson) {
        if (requestJson == null) {
            return null;
        }
        try {
            return JsonUtil.convertValue(JsonUtil.readTree(requestJson.toString()), HttpRequestItem.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize request snapshot: {}", e.getMessage());
            return null;
        }
    }

    private PerformanceRequestSnapshot deserializeRequestSnapshot(JSONObject requestJson) {
        if (requestJson == null) {
            return null;
        }
        try {
            return PerformanceRequestSnapshot.builder()
                    .id(requestJson.getStr("id"))
                    .name(requestJson.getStr("name"))
                    .description(requestJson.getStr("description"))
                    .url(requestJson.getStr("url"))
                    .method(requestJson.getStr("method"))
                    .protocol(enumValue(
                            PerformanceProtocol.class,
                            requestJson.getStr("protocol"),
                            PerformanceProtocol.HTTP
                    ))
                    .headers(deserializeRequestKeyValues(requestJson.getJSONArray("headers")))
                    .bodyType(requestJson.getStr("bodyType"))
                    .body(requestJson.getStr("body"))
                    .params(deserializeRequestKeyValues(requestJson.getJSONArray("params")))
                    .formData(deserializeRequestFormData(requestJson.getJSONArray("formData")))
                    .urlencoded(deserializeRequestKeyValues(requestJson.getJSONArray("urlencoded")))
                    .authType(requestJson.getStr("authType"))
                    .authUsername(requestJson.getStr("authUsername"))
                    .authPassword(requestJson.getStr("authPassword"))
                    .authToken(requestJson.getStr("authToken"))
                    .followRedirects(requestJson.getBool("followRedirects"))
                    .cookieJarEnabled(requestJson.getBool("cookieJarEnabled"))
                    .httpVersion(requestJson.getStr("httpVersion"))
                    .requestTimeoutMs(requestJson.getInt("requestTimeoutMs"))
                    .prescript(requestJson.getStr("prescript"))
                    .postscript(requestJson.getStr("postscript"))
                    .executionScope(deserializeRequestExecutionScopeSnapshot(requestJson.getJSONObject("executionScope")))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to deserialize request snapshot: {}", e.getMessage());
            return null;
        }
    }

    private List<PerformanceRequestKeyValue> deserializeRequestKeyValues(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<PerformanceRequestKeyValue> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject json = array.getJSONObject(i);
            if (json != null) {
                values.add(new PerformanceRequestKeyValue(
                        json.getBool("enabled", true),
                        json.getStr("key", ""),
                        json.getStr("value", "")
                ));
            }
        }
        return values;
    }

    private List<PerformanceRequestFormDataPart> deserializeRequestFormData(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<PerformanceRequestFormDataPart> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject json = array.getJSONObject(i);
            if (json != null) {
                values.add(new PerformanceRequestFormDataPart(
                        json.getBool("enabled", true),
                        json.getStr("key", ""),
                        json.getStr("type", PerformanceRequestFormDataPart.TYPE_TEXT),
                        json.getStr("value", "")
                ));
            }
        }
        return values;
    }

    private PerformanceRequestExecutionScopeSnapshot deserializeRequestExecutionScopeSnapshot(JSONObject json) {
        if (json == null || json.isEmpty()) {
            return PerformanceRequestExecutionScopeSnapshot.empty();
        }
        Map<String, String> groupVariables = new LinkedHashMap<>();
        for (String key : json.keySet()) {
            groupVariables.put(key, json.getStr(key));
        }
        return PerformanceRequestExecutionScopeSnapshot.fromGroupVariables(groupVariables);
    }

    private ThreadGroupData deserializeThreadGroupData(JSONObject json) {
        ThreadGroupData data = new ThreadGroupData();
        try {
            String threadMode = json.getStr("threadMode");
            if (threadMode != null) {
                data.threadMode = ThreadGroupData.ThreadMode.valueOf(threadMode);
            }
            data.numThreads = json.getInt("numThreads", 20);
            data.duration = json.getInt("duration", 60);
            data.loops = json.getInt("loops", 1);
            data.useTime = json.getBool("useTime", true);
            data.rampUpStartThreads = json.getInt("rampUpStartThreads", 1);
            data.rampUpEndThreads = json.getInt("rampUpEndThreads", 20);
            data.rampUpTime = json.getInt("rampUpTime", 30);
            data.rampUpDuration = json.getInt("rampUpDuration", 60);
            data.spikeMinThreads = json.getInt("spikeMinThreads", 1);
            data.spikeMaxThreads = json.getInt("spikeMaxThreads", 20);
            data.spikeRampUpTime = json.getInt("spikeRampUpTime", 20);
            data.spikeHoldTime = json.getInt("spikeHoldTime", 15);
            data.spikeRampDownTime = json.getInt("spikeRampDownTime", 20);
            data.spikeDuration = json.getInt("spikeDuration", 60);
            data.stairsStartThreads = json.getInt("stairsStartThreads", 5);
            data.stairsEndThreads = json.getInt("stairsEndThreads", 20);
            data.stairsStep = json.getInt("stairsStep", 5);
            data.stairsHoldTime = json.getInt("stairsHoldTime", 15);
            data.stairsDuration = json.getInt("stairsDuration", 60);
        } catch (Exception e) {
            log.warn("Failed to deserialize thread group data: {}", e.getMessage());
        }
        data.normalize();
        return data;
    }

    private LoopData deserializeLoopData(JSONObject json) {
        LoopData data = new LoopData();
        if (json != null) {
            data.iterations = json.getInt("iterations", data.iterations);
        }
        data.normalize();
        return data;
    }

    private AssertionData deserializeAssertionData(JSONObject json) {
        AssertionData data = new AssertionData();
        data.type = json.getStr("type", "Response Code");
        data.content = json.getStr("content", "");
        data.operator = json.getStr("operator", "=");
        data.value = json.getStr("value", "200");
        return data;
    }

    private ExtractorData deserializeExtractorData(JSONObject json) {
        ExtractorData data = new ExtractorData();
        data.type = json.getStr("type", data.type);
        data.expression = json.getStr("expression", "");
        data.variableName = json.getStr("variableName", "");
        data.defaultValue = json.getStr("defaultValue", "");
        data.matchIndex = json.getInt("matchIndex", 1);
        data.groupIndex = json.getInt("groupIndex", 1);
        return data;
    }

    private SsePerformanceData deserializeSsePerformanceData(JSONObject json) {
        SsePerformanceData data = new SsePerformanceData();
        try {
            data.completionMode = enumValue(
                    SsePerformanceData.CompletionMode.class,
                    json.getStr("completionMode"),
                    data.completionMode
            );
            data.connectTimeoutMs = json.getInt("connectTimeoutMs", data.connectTimeoutMs);
            data.firstMessageTimeoutMs = json.getInt("firstMessageTimeoutMs", data.firstMessageTimeoutMs);
            data.holdConnectionMs = json.getInt("holdConnectionMs", data.holdConnectionMs);
            data.targetMessageCount = json.getInt("targetMessageCount", data.targetMessageCount);
            data.eventNameFilter = json.getStr("eventNameFilter", data.eventNameFilter);
            data.messageFilter = json.getStr("messageFilter", data.messageFilter);
        } catch (Exception e) {
            log.warn("Failed to deserialize SSE performance data: {}", e.getMessage());
        }
        return data;
    }

    private WebSocketPerformanceData deserializeWebSocketPerformanceData(JSONObject json) {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        try {
            data.connectTimeoutMs = json.getInt("connectTimeoutMs", data.connectTimeoutMs);
            data.sendMode = enumValue(
                    WebSocketPerformanceData.SendMode.class,
                    json.getStr("sendMode"),
                    data.sendMode
            );
            data.sendContentSource = enumValue(
                    WebSocketPerformanceData.SendContentSource.class,
                    json.getStr("sendContentSource"),
                    data.sendContentSource
            );
            data.customSendBody = json.getStr("customSendBody", data.customSendBody);
            data.sendPreScript = json.getStr("sendPreScript", data.sendPreScript);
            data.sendCount = json.getInt("sendCount", data.sendCount);
            data.sendIntervalMs = json.getInt("sendIntervalMs", data.sendIntervalMs);
            data.completionMode = enumValue(
                    WebSocketPerformanceData.CompletionMode.class,
                    json.getStr("completionMode"),
                    data.completionMode
            );
            data.firstMessageTimeoutMs = json.getInt("firstMessageTimeoutMs", data.firstMessageTimeoutMs);
            data.holdConnectionMs = json.getInt("holdConnectionMs", data.holdConnectionMs);
            data.targetMessageCount = json.getInt("targetMessageCount", data.targetMessageCount);
            data.messageFilter = json.getStr("messageFilter", data.messageFilter);
        } catch (Exception e) {
            log.warn("Failed to deserialize WebSocket performance data: {}", e.getMessage());
        }
        return data;
    }

    private TimerData deserializeTimerData(JSONObject json) {
        TimerData data = new TimerData();
        data.delayMs = json.getInt("delayMs", 1000);
        return data;
    }

    private CsvDataSetData deserializeCsvDataSetData(JSONObject json) {
        if (json == null) {
            return null;
        }
        JSONArray headersJson = json.getJSONArray("headers");
        JSONArray rowsJson = json.getJSONArray("rows");
        if (headersJson == null || rowsJson == null || rowsJson.isEmpty()) {
            return null;
        }

        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headersJson.size(); i++) {
            headers.add(headersJson.getStr(i));
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < rowsJson.size(); i++) {
            JSONObject rowJson = rowsJson.getJSONObject(i);
            Map<String, String> row = new LinkedHashMap<>();
            if (rowJson != null) {
                for (String header : headers) {
                    row.put(header, rowJson.getStr(header, ""));
                }
            }
            rows.add(row);
        }

        return new CsvDataSetData(json.getStr("sourceName"), headers, rows);
    }

    private PerformanceCsvState deserializeCsvState(JSONObject json) {
        if (json == null) {
            return null;
        }
        JSONArray headersJson = json.getJSONArray("headers");
        JSONArray rowsJson = json.getJSONArray("rows");
        if (headersJson == null || rowsJson == null || rowsJson.isEmpty()) {
            return null;
        }

        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headersJson.size(); i++) {
            headers.add(headersJson.getStr(i));
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 0; i < rowsJson.size(); i++) {
            JSONObject rowJson = rowsJson.getJSONObject(i);
            Map<String, String> row = new LinkedHashMap<>();
            if (rowJson != null) {
                for (String header : headers) {
                    row.put(header, rowJson.getStr(header, ""));
                }
            }
            rows.add(row);
        }

        return new PerformanceCsvState(json.getStr("sourceName"), headers, rows);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private void deleteFile(File file) {
        try {
            if (file.exists() && !file.delete()) {
                log.warn("Failed to delete config file: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error deleting config file: {}", e.getMessage());
        }
    }
}
