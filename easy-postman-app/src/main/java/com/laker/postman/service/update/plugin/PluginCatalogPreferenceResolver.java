package com.laker.postman.service.update.plugin;

import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.service.update.source.UpdateSource;
import com.laker.postman.service.update.source.UpdateSourceSelector;

import java.util.Locale;

/**
 * 根据自动更新设置解析插件市场目录地址。
 */
public final class PluginCatalogPreferenceResolver {

    private static final UpdateSourceSelector UPDATE_SOURCE_SELECTOR = new UpdateSourceSelector();

    private PluginCatalogPreferenceResolver() {
    }

    public static String resolveEffectiveCatalogUrl() {
        return resolveEffectiveCatalogUrl(true);
    }

    public static String resolveEffectiveCatalogUrl(boolean autoDetectSource) {
        String savedCatalogUrl = PluginManagementService.getCatalogUrl();
        if (savedCatalogUrl == null || savedCatalogUrl.isBlank()) {
            return resolvePreferredOfficialCatalogUrl(autoDetectSource);
        }

        String officialSource = PluginManagementService.detectOfficialCatalogSource(savedCatalogUrl);
        if (!officialSource.isBlank()) {
            return resolvePreferredOfficialCatalogUrl(autoDetectSource);
        }
        return savedCatalogUrl.trim();
    }

    public static String resolvePreferredOfficialCatalogUrl() {
        return resolvePreferredOfficialCatalogUrl(true);
    }

    public static String resolvePreferredOfficialCatalogUrl(boolean autoDetectSource) {
        String preference = SettingManager.getUpdateSourcePreference();
        if ("github".equalsIgnoreCase(preference)) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }
        if ("gitee".equalsIgnoreCase(preference)) {
            return PluginManagementService.getOfficialCatalogUrl("gitee");
        }
        if (!autoDetectSource) {
            return PluginManagementService.getOfficialCatalogUrl("gitee");
        }

        UpdateSource source = UPDATE_SOURCE_SELECTOR.selectBestSource();
        String sourceName = source == null ? "" : source.getName().toLowerCase(Locale.ROOT);
        if (sourceName.contains("hub")) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }
        return PluginManagementService.getOfficialCatalogUrl("gitee");
    }
}
