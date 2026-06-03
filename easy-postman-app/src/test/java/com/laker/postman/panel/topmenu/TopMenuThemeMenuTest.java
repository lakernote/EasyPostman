package com.laker.postman.panel.topmenu;

import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.common.themes.ThemeDescriptor;
import com.laker.postman.util.I18nUtil;
import org.testng.annotations.Test;

import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TopMenuThemeMenuTest {

    @Test
    public void shouldBuildThemeMenuFromRegistry() {
        JMenu menu = TopMenuThemeMenu.create();
        List<ThemeDescriptor> themes = SimpleThemeManager.availableThemes();

        assertEquals(menu.getItemCount(), themes.size());
        for (int i = 0; i < themes.size(); i++) {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem) menu.getItem(i);
            ThemeDescriptor theme = themes.get(i);

            assertEquals(item.getText(), I18nUtil.getMessage(theme.nameKey()));
            if (theme.id().equals(SimpleThemeManager.currentThemeId())) {
                assertTrue(item.isSelected());
            }
        }
    }
}
