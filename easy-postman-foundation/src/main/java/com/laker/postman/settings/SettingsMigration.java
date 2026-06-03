package com.laker.postman.settings;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

public record SettingsMigration(int targetVersion, Consumer<Properties> action) {

    public SettingsMigration {
        if (targetVersion <= 0) {
            throw new IllegalArgumentException("Migration target version must be positive");
        }
        Objects.requireNonNull(action, "action");
    }

    public static SettingsMigration toVersion(int targetVersion, Consumer<Properties> action) {
        return new SettingsMigration(targetVersion, action);
    }

    void apply(Properties properties) {
        action.accept(properties);
    }
}
