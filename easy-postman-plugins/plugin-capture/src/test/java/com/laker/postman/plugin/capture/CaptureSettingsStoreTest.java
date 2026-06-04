package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CaptureSettingsStoreTest {

    @Test
    public void shouldStoreCaptureSettingsInPluginStorage() {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        CaptureSettingsStore store = new CaptureSettingsStore(storage);

        store.save(new CaptureSettings("0.0.0.0", 18888, true, "api.example.com or dev.local"));

        CaptureSettings settings = store.load();
        assertEquals(settings.bindHost(), "0.0.0.0");
        assertEquals(settings.bindPort(), 18888);
        assertTrue(settings.syncSystemProxy());
        assertEquals(settings.hostFilter(), "api.example.com or dev.local");

        String json = storage.files.get(CaptureSettingsStore.STORAGE_FILE);
        assertTrue(json.contains("\"bindHost\""));
        assertFalse(json.contains("plugin.capture"));
    }

    @Test
    public void shouldUseDefaultsWhenSettingsFileIsMissing() {
        CaptureSettings settings = new CaptureSettingsStore(new MemoryPluginStorage()).load();

        assertEquals(settings.bindHost(), "127.0.0.1");
        assertEquals(settings.bindPort(), 8888);
        assertFalse(settings.syncSystemProxy());
        assertEquals(settings.hostFilter(), "");
    }

    private static final class MemoryPluginStorage implements PluginStorage {
        private final Map<String, String> files = new HashMap<>();

        @Override
        public Optional<String> readString(String relativePath) {
            return Optional.ofNullable(files.get(relativePath));
        }

        @Override
        public void writeString(String relativePath, String content) {
            files.put(relativePath, content);
        }

        @Override
        public void delete(String relativePath) {
            files.remove(relativePath);
        }

        @Override
        public Path dataDirectory() {
            return Path.of("");
        }
    }
}
