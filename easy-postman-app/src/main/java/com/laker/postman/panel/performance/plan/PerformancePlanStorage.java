package com.laker.postman.panel.performance.plan;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanJsonStorage;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

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

            writeAtomically(configPath, JSONUtil.toJsonPrettyStr(jsonRoot));
        } catch (IOException e) {
            log.error("Failed to save performance test config: {}", e.getMessage(), e);
        }
    }

    public PerformancePlanConfiguration loadConfiguration(Path configPath) {
        if (configPath == null) {
            return null;
        }
        File file = configPath.toFile();
        if (!file.exists()) {
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
            return deserializeConfiguration(jsonRoot, configPath);
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

    private void writeAtomically(Path configPath, String content) throws IOException {
        Path configDir = configPath.getParent();
        Path tempFile = configDir == null
                ? Files.createTempFile(configPath.getFileName().toString(), ".tmp")
                : Files.createTempFile(configDir, configPath.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private PerformancePlanConfiguration deserializeConfiguration(JSONObject jsonRoot, Path configPath) {
        PerformancePlanDocument document = null;
        JSONObject treeJson = jsonRoot.getJSONObject("tree");
        if (treeJson != null) {
            Map<String, Object> treeMap = jsonObjectToMap(treeJson);
            PerformancePlanLegacyRequestHydrator.hydrate(treeMap, configPath);
            PerformanceCorePlanNode coreRootNode = corePlanJsonStorage.fromTreeMap(treeMap);
            document = PerformanceCorePlanAdapter.toAppDocument(new PerformanceCorePlanDocument(coreRootNode));
        }

        return PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(jsonRoot.getBool("efficientMode", true))
                .trendEnabled(jsonRoot.getBool("trendEnabled", true))
                .reportRealtimeEnabled(jsonRoot.getBool("reportRealtimeEnabled", false))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonObjectToMap(JSONObject json) {
        return JsonUtil.convertValue(JsonUtil.readTree(json.toString()), Map.class);
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
