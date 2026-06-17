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
    private static final List<String> REQUIRED_BUTTON_STYLE_KEYS = List.of(
            "[style]Button.easyPostmanPrimary",
            "[style]Button.easyPostmanSecondary",
            "[style]ToggleButton.easyPostmanToggle"
    );

    private LookAndFeel previousLookAndFeel;

    @BeforeMethod
    public void setUp() {
        previousLookAndFeel = UIManager.getLookAndFeel();
        clearThemeAssertionKeys();
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
    public void shouldDefineModernButtonStyleClassesForBuiltInThemes() throws Exception {
        assertDefinesButtonStyleClasses("com/laker/postman/common/themes/EasyLightLaf.properties");
        assertDefinesButtonStyleClasses("com/laker/postman/common/themes/EasyDarkLaf.properties");
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
        assertThemeBrandColors(new Color(53, 116, 240), new Color(43, 67, 113));
        assertThemeSurfaceColors(
                new Color(201, 204, 211),
                new Color(38, 40, 44),
                new Color(30, 31, 34),
                new Color(38, 40, 44),
                new Color(43, 45, 48),
                new Color(30, 31, 34),
                new Color(43, 45, 48),
                new Color(38, 40, 44),
                new Color(43, 45, 48)
        );

        assertNotNull(UIManager.getColor(ThemeColors.CONSOLE_SELECTION_BACKGROUND));
    }

    @Test
    public void shouldExposeIdeaLikeComponentSurfacesAfterLafSetup() {
        assertTrue(EasyLightLaf.setup());
        assertComponentSurfaces(Color.WHITE, new Color(244, 246, 248), new Color(233, 234, 238));
        assertWorkspaceSurfaces(new Color(233, 234, 238), Color.WHITE);
        assertTopChromeSurfaces(new Color(233, 234, 238));

        assertTrue(EasyDarkLaf.setup());
        assertComponentSurfaces(new Color(30, 31, 34), new Color(43, 45, 48), new Color(43, 45, 48));
        assertWorkspaceSurfaces(new Color(38, 40, 44), new Color(30, 31, 34));
        assertTopChromeSurfaces(new Color(38, 40, 44));
    }

    @Test
    public void shouldExposeToolbarToggleSelectedForegroundForReadableAccentSelection() {
        assertTrue(EasyLightLaf.setup());
        assertEquals(UIManager.getColor("ToggleButton.toolbar.selectedForeground"), Color.WHITE);

        assertTrue(EasyDarkLaf.setup());
        assertEquals(UIManager.getColor("ToggleButton.toolbar.selectedForeground"), Color.WHITE);
    }

    @Test
    public void disabledButtonBackgroundShouldRemainVisibleBehindDisabledText() {
        assertTrue(EasyLightLaf.setup());
        assertReadableDisabledButtonContrast();

        assertTrue(EasyDarkLaf.setup());
        assertReadableDisabledButtonContrast();
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

    private void assertDefinesButtonStyleClasses(String resourcePath) throws Exception {
        Properties properties = loadProperties(resourcePath);

        for (String key : REQUIRED_BUTTON_STYLE_KEYS) {
            assertTrue(properties.containsKey(key), resourcePath + " must define " + key);
            String style = properties.getProperty(key);
            assertTrue(!style.contains("buttonType: roundRect"), key + " must avoid pill-like FlatLaf roundRect type");
            assertTrue(style.contains("arc: 8"), key + " must keep a moderate 8px button arc");
        }
    }

    private void clearThemeAssertionKeys() {
        for (String key : REQUIRED_COMPONENT_SURFACE_KEYS) {
            UIManager.getDefaults().remove(key);
        }
        for (String key : List.of(
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_DISABLED,
                ThemeColors.BACKGROUND,
                ThemeColors.SURFACE,
                ThemeColors.WINDOW_CHROME_BACKGROUND,
                ThemeColors.INPUT_BACKGROUND,
                ThemeColors.TAB_BACKGROUND,
                ThemeColors.TAB_SELECTED_BACKGROUND,
                ThemeColors.TAB_HOVER_BACKGROUND,
                ThemeColors.TAB_SEPARATOR,
                ThemeColors.PRIMARY,
                ThemeColors.SELECTION_BACKGROUND,
                ThemeColors.BUTTON_DISABLED_BACKGROUND,
                ThemeColors.CONSOLE_SELECTION_BACKGROUND,
                "Component.accentColor",
                "Component.focusColor",
                "Component.focusedBorderColor",
                "MenuBar.background",
                "TitlePane.background",
                "TitlePane.inactiveBackground",
                "ToggleButton.toolbar.selectedForeground"
        )) {
            UIManager.getDefaults().remove(key);
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
                "TabbedPane.background",
                "TabbedPane.tabAreaBackground"
        )) {
            assertEquals(UIManager.getColor(key), surface, key);
        }
        assertEquals(UIManager.getColor("TabbedPane.contentAreaColor"), tabSeparator);
        assertEquals(UIManager.getColor("TabbedPane.tabSeparatorColor"), tabSeparator);
        assertEquals(UIManager.getColor("TableHeader.background"), tableHeaderBackground);
    }

    private void assertWorkspaceSurfaces(Color background, Color toolWindowBackground) {
        assertEquals(UIManager.getColor(ThemeColors.BACKGROUND), background);
        assertEquals(UIManager.getColor("Panel.background"), background);
        assertEquals(UIManager.getColor("SplitPane.background"), background);
        assertEquals(UIManager.getColor("List.background"), toolWindowBackground);
        assertEquals(UIManager.getColor("Tree.background"), toolWindowBackground);
    }

    private void assertTopChromeSurfaces(Color expected) {
        assertEquals(UIManager.getColor(ThemeColors.WINDOW_CHROME_BACKGROUND), expected);
        assertEquals(UIManager.getColor("MenuBar.background"), expected);
        assertEquals(UIManager.getColor("TitlePane.background"), expected);
        assertEquals(UIManager.getColor("TitlePane.inactiveBackground"), expected);
    }

    private void assertReadableDisabledButtonContrast() {
        Color background = UIManager.getColor(ThemeColors.BUTTON_DISABLED_BACKGROUND);
        Color foreground = UIManager.getColor(ThemeColors.TEXT_DISABLED);

        assertTrue(contrastRatio(background, foreground) >= 2.0,
                "Disabled button background should not collapse into disabled text");
    }

    private double contrastRatio(Color first, Color second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double relativeLuminance(Color color) {
        double red = linearRgb(color.getRed());
        double green = linearRgb(color.getGreen());
        double blue = linearRgb(color.getBlue());
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private double linearRgb(int value) {
        double channel = value / 255.0;
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
