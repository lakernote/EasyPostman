package com.laker.postman.service.update.plugin;

import java.util.List;

/**
 * 插件更新检查结果。
 */
public record PluginUpdateCheckResult(
        List<PluginUpdateCandidate> candidates,
        String errorMessage
) {

    public static PluginUpdateCheckResult success(List<PluginUpdateCandidate> candidates) {
        return new PluginUpdateCheckResult(candidates == null ? List.of() : List.copyOf(candidates), null);
    }

    public static PluginUpdateCheckResult failed(String errorMessage) {
        return new PluginUpdateCheckResult(List.of(), errorMessage == null ? "" : errorMessage);
    }

    public boolean hasUpdates() {
        return candidates != null && !candidates.isEmpty();
    }

    public boolean failed() {
        return errorMessage != null && !errorMessage.isBlank();
    }
}
