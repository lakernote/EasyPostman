package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginHostActions;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SettingsContributionRegistryTest extends AbstractSwingUiTest {

    @BeforeMethod
    public void resetPluginRuntimeBeforeTest() {
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void resetPluginRuntimeAfterTest() {
        PluginRuntime.resetForTests();
    }

    @Test
    public void defaultContributionsShouldPreserveExistingTabOrderAndPanels() {
        List<SettingsContribution> contributions = SettingsContributionRegistry.defaultRegistry().contributions();

        assertEquals(
                contributions.stream().map(SettingsContribution::id).toList(),
                List.of(
                        "general",
                        "request",
                        "proxy",
                        "trusted-material",
                        "webdav-sync",
                        "auto-update",
                        "performance",
                        "client-certificates",
                        "shortcuts"
                )
        );
        assertEquals(
                contributions.stream().map(SettingsContribution::titleKey).toList(),
                List.of(
                        MessageKeys.SETTINGS_GENERAL_TITLE,
                        MessageKeys.SETTINGS_REQUEST_TITLE,
                        MessageKeys.SETTINGS_PROXY_TITLE,
                        MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_TITLE,
                        MessageKeys.SETTINGS_WEBDAV_SYNC_TITLE,
                        MessageKeys.SETTINGS_AUTO_UPDATE_TITLE,
                        MessageKeys.SETTINGS_PERFORMANCE_TITLE,
                        MessageKeys.CERT_TITLE,
                        MessageKeys.SETTINGS_SHORTCUTS_TITLE
                )
        );
        assertTrue(contributions.get(0).createPanel(new SettingsContributionContext(null))
                instanceof UISettingsPanelModern);
        assertTrue(contributions.get(7).createPanel(new SettingsContributionContext(null))
                instanceof ClientCertificateSettingsPanelModern);
    }

    @Test
    public void modernSettingsDialogShouldBuildTabsFromContributions() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("SettingsContributionRegistry.defaultRegistry()"));
        assertFalse(source.contains("new UISettingsPanelModern()"));
        assertFalse(source.contains("new RequestSettingsPanelModern()"));
        assertFalse(source.contains("new ProxySettingsPanelModern()"));
        assertFalse(source.contains("new TrustedCertificatesSettingsPanelModern()"));
        assertFalse(source.contains("new WebDavSyncSettingsPanel()"));
        assertFalse(source.contains("new AutoUpdateSettingsPanel()"));
        assertFalse(source.contains("new PerformanceSettingsPanelModern()"));
        assertFalse(source.contains("new ClientCertificateSettingsPanelModern(this)"));
        assertFalse(source.contains("new ShortcutSettingsPanel()"));
    }

    @Test
    public void defaultRegistryShouldIncludePluginSettingsContributions() {
        PluginRuntime.getRegistry().registerSettingsContribution(new PluginSettingsContribution(
                "plugin-settings",
                "plugin.settings.title",
                900,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                context -> new JLabel("plugin-settings"),
                "plugin-settings-test-messages",
                getClass().getClassLoader()
        ));

        SettingsContribution contribution = SettingsContributionRegistry.defaultRegistry()
                .findById("plugin-settings")
                .orElseThrow();

        assertEquals(contribution.resolveTitle(), "Plugin Settings");
        assertTrue(contribution.createPanel(new SettingsContributionContext(null)) instanceof JLabel);
    }

    @Test
    public void pluginSettingsContributionShouldOverrideBuiltInContributionWithSameId() {
        PluginRuntime.getRegistry().registerSettingsContribution(new PluginSettingsContribution(
                "client-certificates",
                "plugin.settings.title",
                700,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                context -> new JLabel("plugin-client-cert-settings"),
                "plugin-settings-test-messages",
                getClass().getClassLoader()
        ));

        SettingsContributionRegistry registry = SettingsContributionRegistry.defaultRegistry();
        SettingsContribution contribution = registry.findById("client-certificates").orElseThrow();

        assertEquals(registry.contributions().stream()
                .filter(item -> item.id().equals("client-certificates"))
                .count(), 1L);
        assertEquals(contribution.resolveTitle(), "Plugin Settings");
        assertTrue(contribution.createPanel(new SettingsContributionContext(null)) instanceof JLabel);
    }

    @Test
    public void pluginSettingsContributionsShouldReceiveHostActions() {
        AtomicReference<PluginHostActions> hostActionsRef = new AtomicReference<>();
        PluginRuntime.getRegistry().registerSettingsContribution(new PluginSettingsContribution(
                "plugin-settings-actions",
                "plugin.settings.title",
                900,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                context -> {
                    hostActionsRef.set(context.hostActions());
                    return new JLabel("plugin-settings");
                },
                "plugin-settings-test-messages",
                getClass().getClassLoader()
        ));

        SettingsContribution contribution = SettingsContributionRegistry.defaultRegistry()
                .findById("plugin-settings-actions")
                .orElseThrow();

        assertTrue(contribution.createPanel(new SettingsContributionContext(null)) instanceof JLabel);
        assertTrue(hostActionsRef.get() != PluginHostActions.NOOP);
    }
}
