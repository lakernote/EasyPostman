package com.laker.postman.performance.core.run;

import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocumentSanitizer;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value
public class PerformanceRunPlan {
    // 运行态快照只表达可执行语义，不保存 Swing tree、工作区路径或 GUI 展示开关。
    String schemaVersion;
    String generatedBy;
    String generatedAt;
    PerformanceRunEnvironment environment;
    PerformanceRunVariableSet globals;
    PerformanceRunSettings settings;
    PerformanceCorePlanDocument testPlan;
    List<PerformanceRunAsset> assets;

    @Builder
    public PerformanceRunPlan(String schemaVersion,
                              String generatedBy,
                              String generatedAt,
                              PerformanceRunEnvironment environment,
                              PerformanceRunVariableSet globals,
                              PerformanceRunSettings settings,
                              PerformanceCorePlanDocument testPlan,
                              List<PerformanceRunAsset> assets) {
        this.schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? PerformanceRunPlanJsonStorage.FORMAT_VERSION
                : schemaVersion;
        this.generatedBy = generatedBy == null ? "" : generatedBy;
        this.generatedAt = generatedAt == null || generatedAt.isBlank()
                ? Instant.now().toString()
                : generatedAt;
        PerformanceCorePlanDocument executableTestPlan = PerformanceCorePlanDocumentSanitizer.enabledOnly(testPlan);
        this.environment = sanitizeEnvironment(environment);
        this.globals = sanitizeVariableSet(globals);
        this.settings = settings == null ? PerformanceRunSettings.defaults() : settings;
        this.testPlan = executableTestPlan;
        this.assets = sanitizeAssets(assets, executableTestPlan);
    }

    private static PerformanceRunEnvironment sanitizeEnvironment(PerformanceRunEnvironment environment) {
        if (environment == null) {
            return PerformanceRunEnvironment.empty();
        }
        return new PerformanceRunEnvironment(
                environment.getId(),
                environment.getName(),
                enabledVariables(environment.getVariables())
        );
    }

    private static PerformanceRunVariableSet sanitizeVariableSet(PerformanceRunVariableSet variableSet) {
        if (variableSet == null) {
            return PerformanceRunVariableSet.empty();
        }
        return new PerformanceRunVariableSet(enabledVariables(variableSet.getVariables()));
    }

    private static List<PerformanceRunVariable> enabledVariables(List<PerformanceRunVariable> variables) {
        List<PerformanceRunVariable> copy = new ArrayList<>();
        if (variables == null) {
            return List.of();
        }
        for (PerformanceRunVariable variable : variables) {
            if (variable != null && variable.isEnabled()
                    && variable.getKey() != null && !variable.getKey().isBlank()) {
                copy.add(variable);
            }
        }
        return List.copyOf(copy);
    }

    private static List<PerformanceRunAsset> sanitizeAssets(List<PerformanceRunAsset> assets,
                                                            PerformanceCorePlanDocument testPlan) {
        List<PerformanceRunAsset> scanned = PerformanceRunPlanAssetScanner.scan(testPlan);
        if (scanned.isEmpty()) {
            return List.of();
        }
        Map<String, PerformanceRunAsset> originalByKey = new LinkedHashMap<>();
        for (PerformanceRunAsset asset : copyAssets(assets)) {
            originalByKey.put(assetKey(asset), asset);
        }
        List<PerformanceRunAsset> result = new ArrayList<>();
        for (PerformanceRunAsset asset : scanned) {
            PerformanceRunAsset original = originalByKey.get(assetKey(asset));
            result.add(original == null ? asset : original);
        }
        return List.copyOf(result);
    }

    private static List<PerformanceRunAsset> copyAssets(List<PerformanceRunAsset> assets) {
        List<PerformanceRunAsset> copy = new ArrayList<>();
        if (assets == null) {
            return List.of();
        }
        for (PerformanceRunAsset asset : assets) {
            if (asset != null) {
                copy.add(asset);
            }
        }
        return List.copyOf(copy);
    }

    private static String assetKey(PerformanceRunAsset asset) {
        if (asset == null) {
            return "";
        }
        return asset.getType() + ":" + asset.getPath();
    }
}
