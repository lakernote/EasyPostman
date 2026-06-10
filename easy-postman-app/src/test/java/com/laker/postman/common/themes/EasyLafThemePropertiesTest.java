package com.laker.postman.common.themes;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class EasyLafThemePropertiesTest {
    private static final List<String> REQUIRED_COMPONENT_SURFACE_KEYS = List.of(
            "Panel.background",
            "ToolBar.background",
            "ScrollPane.background",
            "SplitPane.background",
            "TextArea.background",
            "TextPane.background",
            "EditorPane.background",
            "Table.background",
            "TableHeader.background",
            "List.background",
            "Tree.background",
            "TabbedPane.background",
            "TabbedPane.tabAreaBackground",
            "TabbedPane.contentAreaColor",
            "TabbedPane.selectedBackground",
            "TabbedPane.hoverColor",
            "TabbedPane.tabSeparatorColor"
    );

    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void setUp() {
        previousLookAndFeel = UIManager.getLookAndFeel();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (previousLookAndFeel != null) {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    public void shouldDefineSharedSemanticColorDefaultsForBuiltInThemes() throws Exception {
        assertDefinesThemeColors("com/laker/postman/common/themes/EasyLightLaf.properties");
        assertDefinesThemeColors("com/laker/postman/common/themes/EasyDarkLaf.properties");
    }

    @Test
    public void shouldDefineSharedComponentSurfaceDefaultsForBuiltInThemes() throws Exception {
        assertDefinesComponentSurfaceColors("com/laker/postman/common/themes/EasyLightLaf.properties");
        assertDefinesComponentSurfaceColors("com/laker/postman/common/themes/EasyDarkLaf.properties");
    }

    @Test
    public void shouldExposeSemanticColorsThroughUiManagerAfterLafSetup() {
        assertTrue(EasyLightLaf.setup());
        assertThemeBrandColors(new Color(55, 113, 225), new Color(212, 227, 255));
        assertThemeSurfaceColors(
                new Color(15, 23, 42),
                new Color(233, 234, 238),
                Color.WHITE,
                new Color(233, 234, 238),
                Color.WHITE,
                Color.WHITE,
                new Color(226, 235, 254),
                new Color(242, 246, 255),
                new Color(233, 234, 238)
        );

        assertTrue(EasyDarkLaf.setup());
        assertThemeBrandColors(new Color(55, 113, 225), new Color(45, 64, 95));
        assertThemeSurfaceColors(
                new Color(206, 211, 218),
                new Color(39, 41, 44),
                new Color(25, 26, 28),
                new Color(39, 41, 44),
                new Color(39, 41, 44),
                new Color(25, 26, 28),
                new Color(39, 41, 44),
                new Color(34, 36, 40),
                new Color(49, 51, 56)
        );

        assertNotNull(UIManager.getColor(ThemeColors.CONSOLE_SELECTION_BACKGROUND));
    }

    @Test
    public void shouldExposeIdeaLikeComponentSurfacesAfterLafSetup() {
        assertTrue(EasyLightLaf.setup());
        assertComponentSurfaces(Color.WHITE, new Color(244, 246, 248), new Color(233, 234, 238));
        assertTopChromeSurfaces(new Color(233, 234, 238));

        assertTrue(EasyDarkLaf.setup());
        assertComponentSurfaces(new Color(25, 26, 28), new Color(39, 41, 44), new Color(49, 51, 56));
        assertTopChromeSurfaces(new Color(39, 41, 44));
    }

    private void assertDefinesThemeColors(String resourcePath) throws Exception {
        Properties properties = loadProperties(resourcePath);

        for (String key : ThemeColors.REQUIRED_KEYS) {
            assertTrue(properties.containsKey(key), resourcePath + " must define " + key);
        }
    }

    private void assertDefinesComponentSurfaceColors(String resourcePath) throws Exception {
        Properties properties = loadProperties(resourcePath);

        for (String key : REQUIRED_COMPONENT_SURFACE_KEYS) {
            assertTrue(properties.containsKey(key), resourcePath + " must define " + key);
        }
    }

    private Properties loadProperties(String resourcePath) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(in != null, "Missing theme properties: " + resourcePath);
            properties.load(in);
        }
        return properties;
    }

    private void assertThemeSurfaceColors(Color textPrimary,
                                          Color background,
                                          Color surface,
                                          Color windowChromeBackground,
                                          Color inputBackground,
                                          Color tabBackground,
                                          Color tabSelectedBackground,
                                          Color tabHoverBackground,
                                          Color tabSeparator) {
        assertEquals(UIManager.getColor(ThemeColors.TEXT_PRIMARY), textPrimary);
        assertEquals(UIManager.getColor(ThemeColors.BACKGROUND), background);
        assertEquals(UIManager.getColor(ThemeColors.SURFACE), surface);
        assertEquals(UIManager.getColor(ThemeColors.WINDOW_CHROME_BACKGROUND), windowChromeBackground);
        assertEquals(UIManager.getColor(ThemeColors.INPUT_BACKGROUND), inputBackground);
        assertEquals(UIManager.getColor(ThemeColors.TAB_BACKGROUND), tabBackground);
        assertEquals(UIManager.getColor(ThemeColors.TAB_SELECTED_BACKGROUND), tabSelectedBackground);
        assertEquals(UIManager.getColor(ThemeColors.TAB_HOVER_BACKGROUND), tabHoverBackground);
        assertEquals(UIManager.getColor(ThemeColors.TAB_SEPARATOR), tabSeparator);
    }

    private void assertThemeBrandColors(Color primary, Color selectionBackground) {
        assertEquals(UIManager.getColor(ThemeColors.PRIMARY), primary);
        assertEquals(UIManager.getColor("Component.accentColor"), primary);
        assertEquals(UIManager.getColor("Component.focusColor"), primary);
        assertEquals(UIManager.getColor("Component.focusedBorderColor"), primary);
        assertEquals(UIManager.getColor(ThemeColors.SELECTION_BACKGROUND), selectionBackground);
    }

    private void assertComponentSurfaces(Color surface, Color tableHeaderBackground, Color tabSeparator) {
        for (String key : List.of(
                "ToolBar.background",
                "ScrollPane.background",
                "TextArea.background",
                "TextPane.background",
                "EditorPane.background",
                "Table.background",
                "List.background",
                "Tree.background",
                "TabbedPane.background",
                "TabbedPane.tabAreaBackground"
        )) {
            assertEquals(UIManager.getColor(key), surface, key);
        }
        assertEquals(UIManager.getColor("TabbedPane.contentAreaColor"), tabSeparator);
        assertEquals(UIManager.getColor("TabbedPane.tabSeparatorColor"), tabSeparator);
        assertEquals(UIManager.getColor("TableHeader.background"), tableHeaderBackground);
    }

    private void assertTopChromeSurfaces(Color expected) {
        assertEquals(UIManager.getColor(ThemeColors.WINDOW_CHROME_BACKGROUND), expected);
        assertEquals(UIManager.getColor("MenuBar.background"), expected);
        assertEquals(UIManager.getColor("TitlePane.background"), expected);
        assertEquals(UIManager.getColor("TitlePane.inactiveBackground"), expected);
    }
}
