package com.laker.postman.performance.plan;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class PerformancePlanStorage {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String DEFAULT_PLAN_ID = "default";
    private static final String DEFAULT_PLAN_NAME = "Test Plan";
    private final PerformanceCorePlanJsonStorage corePlanJsonStorage = new PerformanceCorePlanJsonStorage();

    public void saveConfiguration(Path configPath, PerformancePlanConfiguration configuration) {
        PerformancePlanWorkspace workspace = loadWorkspace(configPath);
        PerformancePlanWorkspace updatedWorkspace = (workspace == null
                ? PerformancePlanWorkspace.builder().build()
                : workspace)
                .updateActiveConfiguration(configuration, DEFAULT_PLAN_NAME);
        saveWorkspace(configPath, updatedWorkspace);
    }

    public void saveWorkspace(Path configPath, PerformancePlanWorkspace workspace) {
        if (configPath == null) {
            return;
        }
        try {
            PerformancePlanWorkspace safeWorkspace = normalizeWorkspace(workspace);
            PerformanceSavedPlan activePlan = safeWorkspace.getActivePlan();
            PerformancePlanConfiguration activeConfiguration = activePlan == null
                    ? PerformancePlanConfiguration.builder().build()
                    : activePlan.toConfiguration();
            ensureDirExists(configPath);
            JSONObject jsonRoot = new JSONObject();
            jsonRoot.set("version", "1.1");
            writeConfiguration(jsonRoot, activeConfiguration);
            jsonRoot.set("activePlanId", safeWorkspace.getActivePlanId());
            List<JSONObject> planJsonList = new ArrayList<>();
            for (PerformanceSavedPlan plan : safeWorkspace.getPlans()) {
                planJsonList.add(toPlanJson(plan));
            }
            jsonRoot.set("plans", planJsonList);

            writeAtomically(configPath, JSONUtil.toJsonPrettyStr(jsonRoot));
        } catch (IOException e) {
            log.error("Failed to save performance test config: {}", e.getMessage(), e);
        }
    }

    public PerformancePlanConfiguration loadConfiguration(Path configPath) {
        PerformancePlanWorkspace workspace = loadWorkspace(configPath);
        return workspace == null ? null : workspace.getActiveConfiguration();
    }

    public PerformancePlanWorkspace loadWorkspace(Path configPath) {
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
            DeserializedWorkspace result = deserializeWorkspace(jsonRoot, configPath);
            if (result.legacyRequestSnapshotsMigrated()) {
                saveWorkspace(configPath, result.workspace());
                log.info("Migrated legacy performance request snapshots: {}", configPath);
            }
            return result.workspace();
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

    private DeserializedWorkspace deserializeWorkspace(JSONObject jsonRoot, Path configPath) {
        List<PerformanceSavedPlan> plans = new ArrayList<>();
        boolean legacyRequestSnapshotsMigrated = false;
        Object plansValue = jsonRoot.get("plans");
        if (plansValue instanceof Iterable<?> iterable) {
            for (Object planValue : iterable) {
                if (planValue instanceof JSONObject planJson) {
                    DeserializedPlan deserializedPlan = deserializePlan(planJson, configPath);
                    plans.add(deserializedPlan.plan());
                    legacyRequestSnapshotsMigrated |= deserializedPlan.legacyRequestSnapshotsMigrated();
                } else if (planValue instanceof Map<?, ?> planMap) {
                    DeserializedPlan deserializedPlan = deserializePlan(new JSONObject(planMap), configPath);
                    plans.add(deserializedPlan.plan());
                    legacyRequestSnapshotsMigrated |= deserializedPlan.legacyRequestSnapshotsMigrated();
                }
            }
        }

        if (plans.isEmpty()) {
            DeserializedPlan activePlan = deserializePlan(jsonRoot, configPath);
            plans.add(PerformanceSavedPlan.builder()
                    .id(jsonRoot.getStr("activePlanId", DEFAULT_PLAN_ID))
                    .name(resolvePlanName(activePlan.plan(), DEFAULT_PLAN_NAME))
                    .planDocument(activePlan.plan().getPlanDocument())
                    .efficientMode(activePlan.plan().isEfficientMode())
                    .trendEnabled(activePlan.plan().isTrendEnabled())
                    .reportRealtimeEnabled(activePlan.plan().isReportRealtimeEnabled())
                    .remoteWorkerSettings(activePlan.plan().getRemoteWorkerSettings())
                    .build());
            legacyRequestSnapshotsMigrated = activePlan.legacyRequestSnapshotsMigrated();
        }

        PerformancePlanWorkspace workspace = PerformancePlanWorkspace.builder()
                .activePlanId(jsonRoot.getStr("activePlanId", plans.get(0).getId()))
                .plans(plans)
                .build();
        return new DeserializedWorkspace(workspace, legacyRequestSnapshotsMigrated);
    }

    private DeserializedPlan deserializePlan(JSONObject jsonRoot, Path configPath) {
        PerformancePlanDocument document = null;
        boolean legacyRequestSnapshotsMigrated = false;
        JSONObject treeJson = jsonRoot.getJSONObject("tree");
        if (treeJson != null) {
            Map<String, Object> treeMap = jsonObjectToMap(treeJson);
            legacyRequestSnapshotsMigrated = PerformancePlanLegacyRequestHydrator.hydrate(treeMap, configPath);
            PerformanceCorePlanNode coreRootNode = corePlanJsonStorage.fromTreeMap(treeMap);
            document = PerformanceCorePlanAdapter.toAppDocument(new PerformanceCorePlanDocument(coreRootNode));
        }

        PerformancePlanConfiguration configuration = PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(jsonRoot.getBool("efficientMode", true))
                .trendEnabled(jsonRoot.getBool("trendEnabled", true))
                .reportRealtimeEnabled(jsonRoot.getBool("reportRealtimeEnabled", false))
                .remoteWorkerSettings(PerformanceRemoteWorkerSettings.builder()
                        .enabled(jsonRoot.getBool("remoteExecutionEnabled", false))
                        .workerEndpoints(jsonRoot.getStr("remoteWorkers", ""))
                        .build())
                .build();
        PerformanceSavedPlan plan = PerformanceSavedPlan.fromConfiguration(
                jsonRoot.getStr("id", DEFAULT_PLAN_ID),
                jsonRoot.getStr("name", resolvePlanName(configuration, DEFAULT_PLAN_NAME)),
                configuration
        );
        return new DeserializedPlan(plan, legacyRequestSnapshotsMigrated);
    }

    private JSONObject toPlanJson(PerformanceSavedPlan plan) {
        JSONObject planJson = new JSONObject();
        PerformanceSavedPlan safePlan = plan == null ? PerformanceSavedPlan.builder().build() : plan;
        planJson.set("id", safePlan.getId());
        planJson.set("name", safePlan.getName());
        writeConfiguration(planJson, safePlan.toConfiguration());
        return planJson;
    }

    private void writeConfiguration(JSONObject json, PerformancePlanConfiguration configuration) {
        PerformancePlanConfiguration safeConfiguration = configuration == null
                ? PerformancePlanConfiguration.builder().build()
                : configuration;
        json.set("efficientMode", safeConfiguration.isEfficientMode());
        json.set("trendEnabled", safeConfiguration.isTrendEnabled());
        json.set("reportRealtimeEnabled", safeConfiguration.isReportRealtimeEnabled());
        PerformanceRemoteWorkerSettings remoteWorkerSettings = safeConfiguration.getRemoteWorkerSettings();
        json.set("remoteExecutionEnabled", remoteWorkerSettings.isEnabled());
        json.set("remoteWorkers", remoteWorkerSettings.getWorkerEndpoints());
        PerformancePlanDocument document = safeConfiguration.getPlanDocument();
        PerformanceCorePlanDocument coreDocument = PerformanceCorePlanAdapter.toCoreDocument(document);
        json.set("tree", corePlanJsonStorage.toTreeMap(coreDocument == null ? null : coreDocument.getRoot()));
    }

    private PerformancePlanWorkspace normalizeWorkspace(PerformancePlanWorkspace workspace) {
        if (workspace != null && !workspace.getPlans().isEmpty()) {
            return workspace;
        }
        PerformanceSavedPlan defaultPlan = PerformanceSavedPlan.builder()
                .id(DEFAULT_PLAN_ID)
                .name(DEFAULT_PLAN_NAME)
                .build();
        return PerformancePlanWorkspace.builder()
                .activePlanId(defaultPlan.getId())
                .plans(List.of(defaultPlan))
                .build();
    }

    private String resolvePlanName(PerformanceSavedPlan plan, String fallback) {
        if (plan != null && plan.getName() != null && !plan.getName().trim().isEmpty()) {
            return plan.getName();
        }
        return resolvePlanName(plan == null ? null : plan.toConfiguration(), fallback);
    }

    private String resolvePlanName(PerformancePlanConfiguration configuration, String fallback) {
        PerformancePlanDocument document = configuration == null ? null : configuration.getPlanDocument();
        String rootName = document == null || document.getRoot() == null ? null : document.getRoot().getName();
        return rootName == null || rootName.trim().isEmpty() ? fallback : rootName.trim();
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

    private record DeserializedWorkspace(PerformancePlanWorkspace workspace,
                                         boolean legacyRequestSnapshotsMigrated) {
    }

    private record DeserializedPlan(PerformanceSavedPlan plan,
                                    boolean legacyRequestSnapshotsMigrated) {
    }
}
