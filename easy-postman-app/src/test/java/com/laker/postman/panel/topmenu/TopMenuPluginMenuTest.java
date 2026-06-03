package com.laker.postman.panel.topmenu;

import com.laker.postman.plugin.api.PluginMenuContribution;
import com.laker.postman.plugin.runtime.PluginRuntime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TopMenuPluginMenuTest {

    @BeforeMethod
    public void resetPluginRuntimeBeforeTest() {
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void resetPluginRuntimeAfterTest() {
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldAppendPluginMenuContributionsInPluginsMenu() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        PluginRuntime.getRegistry().registerMenuContribution(new PluginMenuContribution(
                "plugin-action",
                PluginMenuContribution.PARENT_MENU_PLUGINS,
                "plugin.menu.title",
                900,
                context -> invoked.set(true),
                "plugin-settings-test-messages",
                getClass().getClassLoader()
        ));

        JMenu menu = TopMenuPluginMenu.create(null);
        JMenuItem pluginItem = findItem(menu, "Plugin Action");

        assertNotNull(pluginItem);
        pluginItem.doClick();
        assertTrue(invoked.get());
    }

    @Test
    public void shouldSortPluginMenuContributionsByOrderThenId() {
        PluginRuntime.getRegistry().registerMenuContribution(new PluginMenuContribution(
                "z-action",
                PluginMenuContribution.PARENT_MENU_PLUGINS,
                "plugin.menu.second",
                920,
                context -> {
                },
                "plugin-settings-test-messages",
                getClass().getClassLoader()
        ));
        PluginRuntime.getRegistry().registerMenuContribution(new PluginMenuContribution(
                "a-action",
                PluginMenuContribution.PARENT_MENU_PLUGINS,
                "plugin.menu.first",
                910,
                context -> {
                },
                "plugin-settings-test-messages",
                getClass().getClassLoader()
        ));

        JMenu menu = TopMenuPluginMenu.create(null);

        assertEquals(menu.getItem(menu.getItemCount() - 2).getText(), "First Plugin Action");
        assertEquals(menu.getItem(menu.getItemCount() - 1).getText(), "Second Plugin Action");
    }

    private static JMenuItem findItem(JMenu menu, String text) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && text.equals(item.getText())) {
                return item;
            }
        }
        return null;
    }
}
