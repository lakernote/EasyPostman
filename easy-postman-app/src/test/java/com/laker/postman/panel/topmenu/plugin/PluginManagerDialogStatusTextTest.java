package com.laker.postman.panel.topmenu.plugin;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PluginManagerDialogStatusTextTest {
    private boolean originalChinese;

    @BeforeMethod
    public void useEnglishLocale() {
        originalChinese = I18nUtil.isChinese();
        I18nUtil.setLocale("en");
    }

    @AfterMethod
    public void restoreLocale() {
        I18nUtil.setLocale(originalChinese ? "zh" : "en");
    }

    @Test
    public void listStatusShouldKeepUpgradeActionVisibleForIncompatiblePlugins() {
        assertEquals(
                PluginManagerDialog.compactListStatusText(
                        I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_INCOMPATIBLE_UPGRADE)),
                "Upgrade required"
        );
    }

    @Test
    public void listStatusShouldDropVersionDetailsForMarketplaceBadges() {
        assertEquals(
                PluginManagerDialog.compactListStatusText(
                        I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_INSTALLED, "5.3.18")),
                "Installed"
        );
        assertEquals(
                PluginManagerDialog.compactListStatusText(
                        I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_UPDATE_AVAILABLE, "5.3.18")),
                "Update available"
        );
        assertEquals(
                PluginManagerDialog.compactListStatusText(
                        I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_LOCAL_NEWER, "5.3.18")),
                "Local newer"
        );
    }

    @Test
    public void listStatusShouldLeaveShortBadgesUnchanged() {
        assertEquals(
                PluginManagerDialog.compactListStatusText(
                        I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_STATUS_DISABLED)),
                "Disabled"
        );
    }

    @Test
    public void marketplaceShouldRefreshFromSourceSelectionWithoutManualLoadButton() {
        assertFalse(Arrays.stream(PluginManagerDialog.class.getDeclaredFields())
                .anyMatch(field -> "loadCatalogButton".equals(field.getName())));
    }

    @Test
    public void marketplaceHintsShouldNotMentionRemovedLoadButton() {
        assertFalse(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_HINT).contains("Load Marketplace"));
        assertFalse(I18nUtil.getMessage(MessageKeys.PLUGIN_MANAGER_MARKET_SOURCE_HINT).contains("Load Marketplace"));
    }
}
