package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.variable.VariableType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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

    @Test
    public void pathVariableTooltipShouldUseCompactVariableTooltipStyle() {
        String tooltip = RequestBodyVariableTooltipBuilder.pathVariableTooltip("id", "123");

        assertTrue(tooltip.contains("id"));
        assertTrue(tooltip.contains("123"));
        assertFalse(tooltip.contains("<hr"));
        assertFalse(tooltip.contains("bgcolor="));
        assertFalse(tooltip.contains("border: 1px"));
    }

    @Test
    public void variableTooltipsShouldUseBalancedMinimumWidthLayout() {
        String pathTooltip = RequestBodyVariableTooltipBuilder.pathVariableTooltip("id", "");
        String requestTooltip = RequestBodyVariableTooltipBuilder.variableTooltip("test", "123", VariableType.GROUP);

        assertTrue(pathTooltip.contains("<table width='210'"));
        assertTrue(requestTooltip.contains("<table width='210'"));
        assertFalse(pathTooltip.contains("<br/>"));
        assertFalse(requestTooltip.contains("<br/>"));
    }

    @Test
    public void variableTooltipsShouldUseCompactFontSizes() {
        String tooltip = RequestBodyVariableTooltipBuilder.variableTooltip("test", "123", VariableType.GROUP);
        int largeTooltipFontSize = Math.max(10, FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1).getSize());
        int compactTooltipFontSize = Math.max(10, FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2).getSize());

        assertTrue(tooltip.contains("font-size: " + compactTooltipFontSize + "px"));
        assertFalse(tooltip.contains("font-size: " + largeTooltipFontSize + "px"));
    }
}
