package com.laker.postman.common.themes;

import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ThemeRegistryTest {

    @Test
    public void shouldRegisterBuiltInThemesWithEditorThemesAndDefaultsSources() {
        List<ThemeDescriptor> themes = ThemeRegistry.all();

        assertEquals(themes.size(), 2);

        ThemeDescriptor light = themes.get(0);
        assertEquals(light.id(), ThemeRegistry.LIGHT_THEME_ID);
        assertEquals(light.nameKey(), MessageKeys.MENU_THEME_LIGHT);
        assertFalse(light.dark());
        assertEquals(light.editorThemeResourcePath(), "/themes/easypostman-light.xml");
        assertEquals(light.fallbackEditorThemeResourcePath(), "/org/fife/ui/rsyntaxtextarea/themes/vs.xml");
        assertEquals(light.flatLafDefaultsSource(), "com.laker.postman.common.themes");

        ThemeDescriptor dark = themes.get(1);
        assertEquals(dark.id(), ThemeRegistry.DARK_THEME_ID);
        assertEquals(dark.nameKey(), MessageKeys.MENU_THEME_DARK);
        assertTrue(dark.dark());
        assertEquals(dark.editorThemeResourcePath(), "/themes/easypostman-dark.xml");
        assertEquals(dark.fallbackEditorThemeResourcePath(), "/org/fife/ui/rsyntaxtextarea/themes/dark.xml");
        assertEquals(dark.flatLafDefaultsSource(), "com.laker.postman.common.themes");
    }

    @Test
    public void shouldResolveUnknownThemeToDefault() {
        assertEquals(ThemeRegistry.getOrDefault("missing").id(), ThemeRegistry.DEFAULT_THEME_ID);
        assertEquals(ThemeRegistry.getOrDefault(" DARK ").id(), ThemeRegistry.DARK_THEME_ID);
    }
}
