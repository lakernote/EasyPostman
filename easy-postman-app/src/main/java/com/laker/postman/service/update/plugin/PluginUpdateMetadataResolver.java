package com.laker.postman.service.update.plugin;

import com.laker.postman.plugin.api.PluginUpdateMetadata;
import com.laker.postman.plugin.api.PluginUpdateMetadataContribution;
import com.laker.postman.plugin.host.PluginAccess;
import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Merges plugin-contributed update metadata into the host plugin catalog model.
 */
@Slf4j
@UtilityClass
public class PluginUpdateMetadataResolver {

    public static List<PluginCatalogEntry> mergeWithContributedMetadata(List<PluginCatalogEntry> catalogEntries) {
        List<PluginCatalogEntry> combined = new ArrayList<>();
        if (catalogEntries != null) {
            catalogEntries.stream()
                    .filter(entry -> entry != null && entry.id() != null && !entry.id().isBlank())
                    .forEach(combined::add);
        }
        combined.addAll(loadContributedCatalogEntries());
        return PluginManagementService.selectCatalogEntriesForCurrentHost(combined);
    }

    static List<PluginCatalogEntry> loadContributedCatalogEntries() {
        List<PluginCatalogEntry> entries = new ArrayList<>();
        for (PluginUpdateMetadataContribution contribution : sortedContributions()) {
            try {
                for (PluginUpdateMetadata metadata : contribution.loadMetadata()) {
                    entries.add(toCatalogEntry(metadata));
                }
            } catch (Throwable t) {
                log.warn("Failed to load plugin update metadata contribution: {}", contribution.id(), t);
            }
        }
        return entries;
    }

    private static List<PluginUpdateMetadataContribution> sortedContributions() {
        return PluginAccess.getUpdateMetadataContributions().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PluginUpdateMetadataContribution::order)
                        .thenComparing(PluginUpdateMetadataContribution::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static PluginCatalogEntry toCatalogEntry(PluginUpdateMetadata metadata) {
        return new PluginCatalogEntry(
                metadata.pluginId(),
                metadata.pluginName(),
                metadata.version(),
                metadata.description(),
                metadata.downloadUrl(),
                metadata.homepageUrl(),
                metadata.sha256(),
                metadata.installUrl(),
                metadata.minAppVersion(),
                metadata.maxAppVersion(),
                metadata.minPlatformVersion(),
                metadata.maxPlatformVersion()
        );
    }
}
