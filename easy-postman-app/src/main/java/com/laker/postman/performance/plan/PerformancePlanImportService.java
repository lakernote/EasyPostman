package com.laker.postman.performance.plan;

import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunSettings;
import com.laker.postman.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerformancePlanImportService {
    private static final List<String> LEGACY_SINGLE_PLAN_CONFIG_KEYS = List.of(
            "efficientMode",
            "trendEnabled",
            "reportRealtimeEnabled",
            "remoteExecutionEnabled",
            "remoteWorkers"
    );

    private final PerformancePlanStorage workspaceStorage = new PerformancePlanStorage();
    private final PerformanceRunPlanJsonStorage runPlanStorage = new PerformanceRunPlanJsonStorage();
    private final PerformanceCorePlanJsonStorage corePlanStorage = new PerformanceCorePlanJsonStorage();

    public PerformancePlanImportResult importPlans(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Import file is required");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Import file does not exist: " + path);
        }
        return importPlans(Files.readString(path, StandardCharsets.UTF_8), defaultPlanName(path));
    }

    PerformancePlanImportResult importPlans(String json, String fallbackPlanName) {
        Map<String, Object> root = readRoot(json);
        if (root.containsKey("plans")) {
            return fromWorkspace(workspaceStorage.loadWorkspaceFromJson(json));
        }
        if (root.containsKey("testPlan")) {
            return fromRunPlan(runPlanStorage.fromJson(json), fallbackPlanName);
        }
        if (root.containsKey("tree")) {
            rejectLegacySinglePlanConfig(root);
            return fromCoreDocument(corePlanStorage.fromJson(json), fallbackPlanName, true);
        }
        throw new IllegalArgumentException("Unsupported performance plan file");
    }

    private void rejectLegacySinglePlanConfig(Map<String, Object> root) {
        for (String key : LEGACY_SINGLE_PLAN_CONFIG_KEYS) {
            if (root.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Unsupported legacy single-plan performance config. Please import a workspace config or exported plan.json."
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRoot(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Import file is empty");
        }
        return JsonUtil.convertValue(JsonUtil.readTree(json), Map.class);
    }

    private PerformancePlanImportResult fromWorkspace(PerformancePlanWorkspace workspace) {
        if (workspace == null || workspace.getPlans().isEmpty()) {
            return new PerformancePlanImportResult(List.of());
        }
        List<PerformancePlanImportCandidate> candidates = new ArrayList<>();
        for (PerformanceSavedPlan plan : workspace.getPlans()) {
            if (plan != null) {
                candidates.add(new PerformancePlanImportCandidate(plan.getName(), plan.toConfiguration()));
            }
        }
        return new PerformancePlanImportResult(candidates);
    }

    private PerformancePlanImportResult fromRunPlan(PerformanceRunPlan runPlan, String fallbackPlanName) {
        if (runPlan == null) {
            return new PerformancePlanImportResult(List.of());
        }
        PerformancePlanDocument document = PerformanceCorePlanAdapter.toAppDocument(runPlan.getTestPlan());
        PerformanceRunSettings settings = runPlan.getSettings() == null
                ? PerformanceRunSettings.defaults()
                : runPlan.getSettings();
        PerformancePlanConfiguration configuration = PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(settings.isEfficientMode())
                .build();
        return new PerformancePlanImportResult(List.of(
                new PerformancePlanImportCandidate(planName(document, fallbackPlanName), configuration)
        ));
    }

    private PerformancePlanImportResult fromCoreDocument(PerformanceCorePlanDocument coreDocument,
                                                         String fallbackPlanName,
                                                         boolean defaultSettings) {
        PerformancePlanDocument document = PerformanceCorePlanAdapter.toAppDocument(coreDocument);
        if (document == null) {
            return new PerformancePlanImportResult(List.of());
        }
        PerformancePlanConfiguration configuration = PerformancePlanConfiguration.builder()
                .planDocument(document)
                .efficientMode(defaultSettings)
                .build();
        return new PerformancePlanImportResult(List.of(
                new PerformancePlanImportCandidate(planName(document, fallbackPlanName), configuration)
        ));
    }

    private String planName(PerformancePlanDocument document, String fallbackPlanName) {
        String rootName = document == null || document.getRoot() == null ? null : document.getRoot().getName();
        if (hasText(rootName)) {
            return rootName.trim();
        }
        if (hasText(fallbackPlanName)) {
            return fallbackPlanName.trim();
        }
        return "Imported Plan";
    }

    private String defaultPlanName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "Imported Plan";
        }
        String fileName = path.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex <= 0 ? fileName : fileName.substring(0, extensionIndex);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
