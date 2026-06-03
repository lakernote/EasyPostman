package com.laker.postman.settings;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

@Slf4j
public final class PreferencesStore {

    public static final String DEFAULT_SCHEMA_VERSION_KEY = "settings.schema.version";

    private final Path file;
    private final Properties properties;
    private final Object lock;
    private final String schemaVersionKey;

    private PreferencesStore(Path file, Properties properties, Object lock, String schemaVersionKey) {
        this.file = Objects.requireNonNull(file, "file");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.lock = Objects.requireNonNull(lock, "lock");
        this.schemaVersionKey = Objects.requireNonNull(schemaVersionKey, "schemaVersionKey");
    }

    public static PreferencesStore fileBacked(Path file) {
        return backedBy(file, new Properties(), new Object());
    }

    public static PreferencesStore backedBy(Path file, Properties properties, Object lock) {
        return new PreferencesStore(file, properties, lock, DEFAULT_SCHEMA_VERSION_KEY);
    }

    public void load() {
        synchronized (lock) {
            refreshInMemory(loadProperties(file));
        }
    }

    public void loadAndMigrate(Collection<SettingsMigration> migrations) {
        synchronized (lock) {
            Properties loaded = loadProperties(file);
            applyPendingMigrations(loaded, migrations);
            refreshInMemory(loaded);
        }
    }

    public void save() {
        synchronized (lock) {
            Properties merged = loadProperties(file);
            merged.putAll(properties);
            storeProperties(merged, file);
            refreshInMemory(merged);
        }
    }

    public void updateAndSave(Consumer<Properties> updater) {
        Objects.requireNonNull(updater, "updater");
        synchronized (lock) {
            Properties merged = loadProperties(file);
            merged.putAll(properties);
            updater.accept(merged);
            storeProperties(merged, file);
            refreshInMemory(merged);
        }
    }

    public <T> T get(SettingKey<T> key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return key.read(properties);
        }
    }

    public <T> void put(SettingKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        updateAndSave(settings -> key.write(settings, value));
    }

    public boolean contains(SettingKey<?> key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return properties.containsKey(key.name());
        }
    }

    public int getSchemaVersion() {
        synchronized (lock) {
            return readSchemaVersion(properties);
        }
    }

    public Properties snapshot() {
        synchronized (lock) {
            Properties copy = new Properties();
            copy.putAll(properties);
            return copy;
        }
    }

    public static Properties loadProperties(Path file) {
        Properties loaded = new Properties();
        if (file == null || !Files.exists(file)) {
            return loaded;
        }
        try (InputStream input = Files.newInputStream(file)) {
            loaded.load(input);
        } catch (IOException e) {
            log.warn("Failed to load preferences from {}", file, e);
        }
        return loaded;
    }

    public static void storeProperties(Properties properties, Path file) {
        if (file == null) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(file)) {
                properties.store(output, "EasyPostman Settings");
            }
        } catch (IOException e) {
            log.warn("Failed to save preferences to {}", file, e);
        }
    }

    private void applyPendingMigrations(Properties loaded, Collection<SettingsMigration> migrations) {
        if (migrations == null || migrations.isEmpty()) {
            return;
        }
        int currentVersion = readSchemaVersion(loaded);
        List<SettingsMigration> pending = migrations.stream()
                .filter(migration -> migration.targetVersion() > currentVersion)
                .sorted(Comparator.comparingInt(SettingsMigration::targetVersion))
                .toList();
        if (pending.isEmpty()) {
            return;
        }

        int migratedVersion = currentVersion;
        for (SettingsMigration migration : pending) {
            migration.apply(loaded);
            migratedVersion = migration.targetVersion();
        }
        loaded.setProperty(schemaVersionKey, String.valueOf(migratedVersion));
        storeProperties(loaded, file);
    }

    private int readSchemaVersion(Properties source) {
        String value = source.getProperty(schemaVersionKey);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refreshInMemory(Properties source) {
        properties.clear();
        properties.putAll(source);
    }
}
