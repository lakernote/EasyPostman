package com.laker.postman.service.setting;

import com.laker.postman.settings.SettingsMigration;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Properties;

@UtilityClass
class AppSettingsMigrations {

    static final int LATEST_SCHEMA_VERSION = 1;
    private static final String LEGACY_UPDATE_SOURCE = "update_source";

    static List<SettingsMigration> migrations() {
        return List.of(
                SettingsMigration.toVersion(LATEST_SCHEMA_VERSION, AppSettingsMigrations::migrateLegacyUpdateSource)
        );
    }

    private static void migrateLegacyUpdateSource(Properties settings) {
        migrateKey(settings, LEGACY_UPDATE_SOURCE, AppSettingKeys.UPDATE_SOURCE_PREFERENCE.name());
    }

    private static void migrateKey(Properties settings, String oldKey, String newKey) {
        if (!settings.containsKey(oldKey)) {
            return;
        }
        if (!settings.containsKey(newKey)) {
            settings.setProperty(newKey, settings.getProperty(oldKey));
        }
        settings.remove(oldKey);
    }
}
