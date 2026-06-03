package com.laker.postman.plugin.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 插件私有持久化空间。
 * <p>
 * 插件只能使用相对路径读写自己的数据目录，不应该直接依赖宿主 app 的设置文件或内部路径。
 * </p>
 */
public interface PluginStorage {

    Optional<String> readString(String relativePath) throws IOException;

    void writeString(String relativePath, String content) throws IOException;

    void delete(String relativePath) throws IOException;

    Path dataDirectory();

    static PluginStorage noop() {
        return NoopPluginStorage.INSTANCE;
    }

    final class NoopPluginStorage implements PluginStorage {
        private static final NoopPluginStorage INSTANCE = new NoopPluginStorage();

        private NoopPluginStorage() {
        }

        @Override
        public Optional<String> readString(String relativePath) {
            return Optional.empty();
        }

        @Override
        public void writeString(String relativePath, String content) {
            // no-op for compatibility with tests or standalone plugin panel construction
        }

        @Override
        public void delete(String relativePath) {
            // no-op for compatibility with tests or standalone plugin panel construction
        }

        @Override
        public Path dataDirectory() {
            return Path.of("");
        }
    }
}
