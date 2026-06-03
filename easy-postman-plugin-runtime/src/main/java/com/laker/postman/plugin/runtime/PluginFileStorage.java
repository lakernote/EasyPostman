package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.PluginStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class PluginFileStorage implements PluginStorage {

    private final Path root;

    static PluginFileStorage forPlugin(String pluginId) {
        return new PluginFileStorage(PluginRuntimePaths.pluginDataDir(pluginId).toAbsolutePath().normalize());
    }

    @Override
    public Optional<String> readString(String relativePath) throws IOException {
        Path path = resolve(relativePath);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("Plugin storage path is not a file: " + relativePath);
        }
        return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
    }

    @Override
    public void writeString(String relativePath, String content) throws IOException {
        Path path = resolve(relativePath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                path,
                content == null ? "" : content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Files.deleteIfExists(resolve(relativePath));
    }

    @Override
    public Path dataDirectory() {
        return root;
    }

    private Path resolve(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("Plugin storage path must not be blank");
        }
        Path relative = Paths.get(relativePath);
        if (relative.isAbsolute()) {
            throw new IOException("Plugin storage path must be relative: " + relativePath);
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Plugin storage path escapes plugin data directory: " + relativePath);
        }
        return resolved;
    }
}
