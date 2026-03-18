package com.laker.postman.plugin.runtime;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 插件候选选择器。
 * <p>
 * 负责从扫描结果里挑出真正应该被 runtime 加载的那一批插件。
 * </p>
 */
@Slf4j
final class PluginCandidateResolver {

    private PluginCandidateResolver() {
    }

    static List<PluginFileInfo> resolveLoadCandidates(Set<Path> pluginDirs,
                                                      Set<String> disabledPluginIds,
                                                      Set<String> pendingUninstallPluginIds,
                                                      Predicate<Path> isLoadedPredicate) {
        Map<String, PluginFileInfo> selected = new LinkedHashMap<>();
        for (Path pluginDir : pluginDirs) {
            for (PluginFileInfo candidate : PluginScanner.listPluginsFromDirectory(
                    pluginDir,
                    disabledPluginIds,
                    pendingUninstallPluginIds,
                    isLoadedPredicate
            )) {
                if (!candidate.enabled()) {
                    if (pendingUninstallPluginIds.contains(candidate.descriptor().id())) {
                        log.info("Skip pending uninstall plugin: {} ({})", candidate.descriptor().id(), candidate.jarPath());
                        continue;
                    }
                    log.info("Skip disabled plugin: {} ({})", candidate.descriptor().id(), candidate.jarPath());
                    continue;
                }
                if (!candidate.compatible()) {
                    PluginCompatibility compatibility = PluginRuntime.evaluateCompatibility(candidate.descriptor());
                    log.warn("Skip incompatible plugin: {} requires app {}..{} and platform {}..{}, current app {}, current platform {}",
                            candidate.descriptor().id(),
                            emptyToWildcard(candidate.descriptor().minAppVersion()),
                            emptyToWildcard(candidate.descriptor().maxAppVersion()),
                            emptyToWildcard(candidate.descriptor().minPlatformVersion()),
                            emptyToWildcard(candidate.descriptor().maxPlatformVersion()),
                            compatibility.currentAppVersion(),
                            compatibility.currentPlatformVersion());
                    continue;
                }
                PluginFileInfo existing = selected.get(candidate.descriptor().id());
                if (existing == null) {
                    selected.put(candidate.descriptor().id(), candidate);
                    continue;
                }
                if (PluginVersionComparator.compare(candidate.descriptor().version(), existing.descriptor().version()) > 0) {
                    selected.put(candidate.descriptor().id(), candidate);
                }
            }
        }
        List<PluginFileInfo> result = new ArrayList<>(selected.values());
        result.sort(Comparator.comparing(info -> info.jarPath().getFileName().toString()));
        return result;
    }

    private static String emptyToWildcard(String value) {
        return value == null || value.isBlank() ? "*" : value;
    }
}
