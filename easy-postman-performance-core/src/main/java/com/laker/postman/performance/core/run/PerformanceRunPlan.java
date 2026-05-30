package com.laker.postman.performance.core.run;

import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        this.environment = environment == null ? PerformanceRunEnvironment.empty() : environment;
        this.globals = globals == null ? PerformanceRunVariableSet.empty() : globals;
        this.settings = settings == null ? PerformanceRunSettings.defaults() : settings;
        this.testPlan = testPlan;
        this.assets = copyAssets(assets);
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
}
