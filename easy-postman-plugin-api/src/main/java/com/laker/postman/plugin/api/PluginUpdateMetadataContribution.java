package com.laker.postman.plugin.api;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy provider for plugin update metadata.
 */
public record PluginUpdateMetadataContribution(
        String id,
        int order,
        Supplier<List<PluginUpdateMetadata>> metadataSupplier
) {

    public PluginUpdateMetadataContribution(String id,
                                            Supplier<List<PluginUpdateMetadata>> metadataSupplier) {
        this(id, 1000, metadataSupplier);
    }

    public PluginUpdateMetadataContribution {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plugin update metadata contribution id must not be blank");
        }
        id = id.trim();
        metadataSupplier = Objects.requireNonNull(metadataSupplier, "metadataSupplier");
    }

    public List<PluginUpdateMetadata> loadMetadata() {
        List<PluginUpdateMetadata> metadata = metadataSupplier.get();
        if (metadata == null || metadata.isEmpty()) {
            return List.of();
        }
        return metadata.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
