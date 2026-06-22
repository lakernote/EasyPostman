package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginSettingsContributionContext;
import com.laker.postman.plugin.host.AppPluginHostActions;
import com.laker.postman.plugin.host.PluginAccess;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for settings dialog tabs.
 */
@Slf4j
public final class SettingsContributionRegistry {

    public static final String CATEGORY_APPLICATION = "application";
    public static final String CATEGORY_NETWORK = "network";
    public static final String CATEGORY_RUNTIME = "runtime";
    public static final String CATEGORY_EXTENSIONS = "extensions";

    private final List<SettingsContribution> contributions;

    public SettingsContributionRegistry(Collection<SettingsContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            this.contributions = List.of();
            return;
        }

        Set<String> ids = new HashSet<>();
        List<SettingsContribution> sortedContributions = new ArrayList<>();
        for (SettingsContribution contribution : contributions) {
            if (contribution == null) {
                continue;
            }
            if (!ids.add(contribution.id())) {
                throw new IllegalArgumentException("Duplicate settings contribution id: " + contribution.id());
            }
            sortedContributions.add(contribution);
        }
        sortedContributions.sort(Comparator
                .comparingInt(SettingsContribution::order)
                .thenComparing(SettingsContribution::id));
        this.contributions = List.copyOf(sortedContributions);
    }

    public static SettingsContributionRegistry defaultRegistry() {
        return new SettingsContributionRegistry(defaultContributions());
    }

    public static List<SettingsContribution> defaultContributions() {
        List<SettingsContribution> pluginContributions = pluginContributions();
        Set<String> pluginIds = new HashSet<>();
        for (SettingsContribution contribution : pluginContributions) {
            pluginIds.add(contribution.id());
        }

        List<SettingsContribution> contributions = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (SettingsContribution contribution : builtInContributions()) {
            if (pluginIds.contains(contribution.id())) {
                log.info("Plugin settings contribution overrides built-in settings contribution id: {}", contribution.id());
                continue;
            }
            ids.add(contribution.id());
            contributions.add(contribution);
        }

        for (SettingsContribution contribution : pluginContributions) {
            if (!ids.add(contribution.id())) {
                log.warn("Skip duplicate plugin settings contribution id: {}", contribution.id());
                continue;
            }
            contributions.add(contribution);
        }
        return List.copyOf(contributions);
    }

    private static List<SettingsContribution> pluginContributions() {
        List<SettingsContribution> contributions = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (PluginSettingsContribution pluginContribution : PluginAccess.getSettingsContributions()) {
            SettingsContribution contribution = adaptPluginContribution(pluginContribution);
            if (!ids.add(contribution.id())) {
                log.warn("Skip duplicate plugin settings contribution id: {}", contribution.id());
                continue;
            }
            contributions.add(contribution);
        }
        return contributions;
    }

    private static List<SettingsContribution> builtInContributions() {
        return List.of(
                new SettingsContribution(
                        "general",
                        MessageKeys.SETTINGS_GENERAL_TITLE,
                        100,
                        CATEGORY_APPLICATION,
                        context -> new UISettingsPanelModern()
                ),
                new SettingsContribution(
                        "request",
                        MessageKeys.SETTINGS_REQUEST_TITLE,
                        200,
                        CATEGORY_APPLICATION,
                        context -> new RequestSettingsPanelModern()
                ),
                new SettingsContribution(
                        "proxy",
                        MessageKeys.SETTINGS_PROXY_TITLE,
                        300,
                        CATEGORY_NETWORK,
                        context -> new ProxySettingsPanelModern()
                ),
                new SettingsContribution(
                        "trusted-material",
                        MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_TITLE,
                        400,
                        CATEGORY_NETWORK,
                        context -> new TrustedCertificatesSettingsPanelModern()
                ),
                new SettingsContribution(
                        "webdav-sync",
                        MessageKeys.SETTINGS_WEBDAV_SYNC_TITLE,
                        450,
                        CATEGORY_NETWORK,
                        context -> new WebDavSyncSettingsPanel()
                ),
                new SettingsContribution(
                        "auto-update",
                        MessageKeys.SETTINGS_AUTO_UPDATE_TITLE,
                        500,
                        CATEGORY_RUNTIME,
                        context -> new AutoUpdateSettingsPanel()
                ),
                new SettingsContribution(
                        "performance",
                        MessageKeys.SETTINGS_PERFORMANCE_TITLE,
                        600,
                        CATEGORY_RUNTIME,
                        context -> new PerformanceSettingsPanelModern()
                ),
                new SettingsContribution(
                        "client-certificates",
                        MessageKeys.CERT_TITLE,
                        700,
                        CATEGORY_EXTENSIONS,
                        context -> new ClientCertificateSettingsPanelModern(context.parentWindow())
                ),
                new SettingsContribution(
                        "shortcuts",
                        MessageKeys.SETTINGS_SHORTCUTS_TITLE,
                        800,
                        CATEGORY_APPLICATION,
                        context -> new ShortcutSettingsPanel()
                )
        );
    }

    private static SettingsContribution adaptPluginContribution(PluginSettingsContribution contribution) {
        return new SettingsContribution(
                contribution.id(),
                contribution.titleKey(),
                contribution.order(),
                contribution.category(),
                context -> contribution.createPanel(new PluginSettingsContributionContext(
                        context.parentWindow(),
                        new AppPluginHostActions(context.parentWindow())
                )),
                contribution.titleBundleName(),
                contribution.titleClassLoader()
        );
    }

    public List<SettingsContribution> contributions() {
        return contributions;
    }

    public Optional<SettingsContribution> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return contributions.stream()
                .filter(contribution -> contribution.id().equals(id))
                .findFirst();
    }

}
