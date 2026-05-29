package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class TimelinePanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.SURFACE,
                ThemeColors.BACKGROUND,
                ThemeColors.BORDER_LIGHT,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_SECONDARY,
                ThemeColors.TEXT_HINT,
                ThemeColors.DIVIDER,
                ThemeColors.PRIMARY,
                ThemeColors.SECONDARY,
                ThemeColors.ACCENT,
                ThemeColors.WARNING,
                ThemeColors.ERROR,
                ThemeColors.SUCCESS
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void surfaceColorsShouldUseSemanticThemeTokens() {
        Color surface = new Color(12, 13, 14);
        Color background = new Color(22, 23, 24);
        Color border = new Color(32, 33, 34);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.BORDER_LIGHT, border);

        assertEquals(TimelineTheme.panelBackground(), surface);
        assertEquals(TimelineTheme.infoBackground(), background);
        assertEquals(TimelineTheme.barAreaBackground(), background);
        assertEquals(TimelineTheme.infoBorder(), border);
    }

    @Test
    public void textColorsShouldUseSemanticThemeTokens() {
        Color label = new Color(42, 43, 44);
        Color info = new Color(52, 53, 54);
        Color description = new Color(62, 63, 64);
        Color separator = new Color(72, 73, 74);
        UIManager.put(ThemeColors.TEXT_PRIMARY, label);
        UIManager.put(ThemeColors.TEXT_SECONDARY, info);
        UIManager.put(ThemeColors.TEXT_HINT, description);
        UIManager.put(ThemeColors.DIVIDER, separator);

        assertEquals(TimelineTheme.labelText(), label);
        assertEquals(TimelineTheme.infoText(), info);
        assertEquals(TimelineTheme.descriptionText(), description);
        assertEquals(TimelineTheme.separator(), separator);
    }

    @Test
    public void barColorsShouldUseSemanticThemeTokens() {
        Color primary = new Color(11, 12, 13);
        Color secondary = new Color(21, 22, 23);
        Color accent = new Color(31, 32, 33);
        Color warning = new Color(41, 42, 43);
        Color error = new Color(51, 52, 53);
        Color success = new Color(61, 62, 63);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.SECONDARY, secondary);
        UIManager.put(ThemeColors.ACCENT, accent);
        UIManager.put(ThemeColors.WARNING, warning);
        UIManager.put(ThemeColors.ERROR, error);
        UIManager.put(ThemeColors.SUCCESS, success);

        assertEquals(TimelineTheme.barColors(), new Color[]{
                primary, secondary, accent, warning, error, success
        });
    }
}
