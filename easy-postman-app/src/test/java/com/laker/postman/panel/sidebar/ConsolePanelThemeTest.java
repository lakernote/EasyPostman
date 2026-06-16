package com.laker.postman.panel.sidebar;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ConsolePanelThemeTest {
    private static final List<String> THEME_TOKEN_KEYS = List.of(
            ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND,
            ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND,
            ThemeColors.TAB_SEPARATOR
    );

    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = new HashMap<>();
        for (String key : THEME_TOKEN_KEYS) {
            previousThemeTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void restoreThemeTokens() {
        for (Map.Entry<String, Object> entry : previousThemeTokens.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void shouldUseSearchHighlightThemeTokens() {
        Color highlight = new Color(80, 81, 82);
        Color currentHighlight = new Color(90, 91, 92);
        UIManager.put(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, highlight);
        UIManager.put(ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND, currentHighlight);

        assertEquals(ConsoleTheme.searchHighlightBackground(), highlight);
        assertEquals(ConsoleTheme.searchCurrentHighlightBackground(), currentHighlight);
    }

    @Test
    public void toolbarControlsShouldKeepCompactWidthWhenToolbarExpands() throws Exception {
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"All"});
        int width = 90;

        Method method = ConsolePanel.class.getDeclaredMethod(
                "lockToolbarControlSize",
                JComponent.class,
                int.class
        );
        method.setAccessible(true);
        method.invoke(null, comboBox, width);

        Dimension expected = new Dimension(width, ToolWindowActionToolbar.ACTION_SIZE);
        assertEquals(comboBox.getMinimumSize(), expected);
        assertEquals(comboBox.getPreferredSize(), expected);
        assertEquals(comboBox.getMaximumSize(), expected);
    }

    @Test
    public void consoleHideActionShouldUsePlainToolWindowHideIcon() throws Exception {
        String iconSvg = readClasspathResource("icons/tool-window-hide.svg");
        assertTrue(iconSvg.contains("viewBox=\"0 0 24 24\""));
        assertTrue(iconSvg.contains("M7 12h10"));
        assertFalse(iconSvg.contains("<circle"));

        Field iconField = ConsolePanel.class.getDeclaredField("TOOL_WINDOW_HIDE_ICON");
        iconField.setAccessible(true);
        assertEquals(iconField.get(null), "icons/tool-window-hide.svg");
    }

    @Test
    public void consoleLogScrollBarShouldAppearOnlyWhenNeeded() {
        JScrollPane scrollPane = findFirstComponent(createLayoutOnlyConsolePanel(), JScrollPane.class);

        assertNotNull(scrollPane);
        assertEquals(scrollPane.getVerticalScrollBarPolicy(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    @Test
    public void consoleToolbarShouldSeparateActionsFromLogContent() {
        Color separator = new Color(229, 231, 235);
        UIManager.put(ThemeColors.TAB_SEPARATOR, separator);
        ConsolePanel consolePanel = createLayoutOnlyConsolePanel();
        JComponent topComponent = (JComponent) ((BorderLayout) consolePanel.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);

        assertTrue(topComponent.getBorder() instanceof CompoundBorder);
        CompoundBorder border = (CompoundBorder) topComponent.getBorder();
        assertTrue(border.getOutsideBorder() instanceof MatteBorder);
        MatteBorder separatorBorder = (MatteBorder) border.getOutsideBorder();
        assertEquals(separatorBorder.getBorderInsets(topComponent).bottom, 1);
        assertEquals(separatorBorder.getMatteColor(), separator);
    }

    private static ConsolePanel createLayoutOnlyConsolePanel() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            ConsolePanel panel = new ConsolePanel();
            panel.initUI();
            return panel;
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    private static String readClasspathResource(String path) throws IOException {
        try (InputStream input = ConsolePanelThemeTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Missing classpath resource: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static <T extends Component> T findFirstComponent(Container container, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container childContainer) {
                T nested = findFirstComponent(childContainer, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
