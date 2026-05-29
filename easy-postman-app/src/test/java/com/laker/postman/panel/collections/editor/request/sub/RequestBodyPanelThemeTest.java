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

public class RequestBodyPanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.CONSOLE_SELECTION_BACKGROUND,
                ThemeColors.BORDER_MEDIUM,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_HINT,
                ThemeColors.HOVER_BACKGROUND
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void popupSelectionShouldUseSemanticSelectionBackground() {
        Color selection = new Color(41, 42, 43);
        UIManager.put(ThemeColors.CONSOLE_SELECTION_BACKGROUND, selection);

        assertEquals(RequestBodyTheme.popupSelectionBackground(), selection);
    }

    @Test
    public void tooltipColorsShouldUseSemanticTextAndSurfaceTokens() {
        Color divider = new Color(51, 52, 53);
        Color text = new Color(61, 62, 63);
        Color muted = new Color(71, 72, 73);
        Color codeBackground = new Color(81, 82, 83);
        UIManager.put(ThemeColors.BORDER_MEDIUM, divider);
        UIManager.put(ThemeColors.TEXT_PRIMARY, text);
        UIManager.put(ThemeColors.TEXT_HINT, muted);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, codeBackground);

        assertEquals(RequestBodyTheme.tooltipDivider(), divider);
        assertEquals(RequestBodyTheme.tooltipText(), text);
        assertEquals(RequestBodyTheme.tooltipMutedText(), muted);
        assertEquals(RequestBodyTheme.tooltipCodeBackground(), codeBackground);
    }
}
