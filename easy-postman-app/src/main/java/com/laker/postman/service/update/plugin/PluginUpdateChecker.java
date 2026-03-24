package com.laker.postman.service.update.plugin;

import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import com.laker.postman.plugin.runtime.PluginCompatibility;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.service.update.version.VersionComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件更新检查器。
 */
public class PluginUpdateChecker {

    public PluginUpdateChecker() {
    }

    public List<PluginUpdateCandidate> checkForUpdates() throws Exception {
        String catalogUrl = resolveCatalogUrl();
        List<PluginCatalogEntry> catalogEntries = loadCatalogWithFallback(catalogUrl);
        return findUpdateCandidates(PluginManagementService.getInstalledPlugins(), catalogEntries);
    }

    String resolveCatalogUrl() {
        return PluginCatalogPreferenceResolver.resolveEffectiveCatalogUrl();
    }

    List<PluginCatalogEntry> loadCatalogWithFallback(String catalogUrl) throws Exception {
        try {
            return PluginManagementService.loadCatalog(catalogUrl);
        } catch (Exception remoteError) {
            String source = PluginManagementService.detectOfficialCatalogSource(catalogUrl);
            if (source.isBlank()) {
                throw remoteError;
            }
            return PluginManagementService.loadBundledOfficialCatalog(source);
        }
    }

    static List<PluginUpdateCandidate> findUpdateCandidates(List<PluginFileInfo> installedPlugins,
                                                            List<PluginCatalogEntry> catalogEntries) {
        if (installedPlugins == null || installedPlugins.isEmpty() || catalogEntries == null || catalogEntries.isEmpty()) {
            return List.of();
        }

        Map<String, PluginFileInfo> installedMap = buildInstalledPluginMap(installedPlugins);
        List<PluginUpdateCandidate> candidates = new ArrayList<>();
        for (PluginCatalogEntry entry : catalogEntries) {
            PluginFileInfo installed = installedMap.get(entry.id());
            if (installed == null || PluginManagementService.isPluginPendingUninstall(installed.descriptor().id())) {
                continue;
            }
            PluginCompatibility compatibility = PluginManagementService.evaluateCompatibility(entry);
            if (!compatibility.compatible()) {
                continue;
            }
            if (VersionComparator.compare(entry.version(), installed.descriptor().version()) > 0) {
                candidates.add(new PluginUpdateCandidate(
                        entry.id(),
                        entry.name(),
                        installed.descriptor().version(),
                        entry.version()
                ));
            }
        }

        candidates.sort(Comparator.comparing(PluginUpdateCandidate::pluginName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PluginUpdateCandidate::pluginId, String.CASE_INSENSITIVE_ORDER));
        return candidates;
    }

    private static Map<String, PluginFileInfo> buildInstalledPluginMap(List<PluginFileInfo> plugins) {
        Map<String, PluginFileInfo> map = new LinkedHashMap<>();
        for (PluginFileInfo info : plugins) {
            if (info == null || info.descriptor() == null || info.descriptor().id() == null
                    || info.descriptor().id().isBlank()) {
                continue;
            }
            PluginFileInfo existing = map.get(info.descriptor().id());
            if (existing == null || VersionComparator.compare(
                    info.descriptor().version(),
                    existing.descriptor().version()
            ) > 0) {
                map.put(info.descriptor().id(), info);
            }
        }
        return map;
    }
}
