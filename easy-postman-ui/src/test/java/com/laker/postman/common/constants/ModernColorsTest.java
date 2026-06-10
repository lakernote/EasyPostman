package com.laker.postman.common.constants;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ModernColorsTest {
    private static final List<String> THEME_TOKEN_KEYS = List.of(
            ThemeColors.PRIMARY,
            ThemeColors.PRIMARY_DARK,
            ThemeColors.PRIMARY_DARKER,
            ThemeColors.SUCCESS,
            ThemeColors.SELECTION_BACKGROUND,
            ThemeColors.GIT_COMMIT,
            ThemeColors.GIT_PUSH,
            ThemeColors.GIT_PULL,
            ThemeColors.VARIABLE_CONTEXT,
            ThemeColors.VARIABLE_ITERATION_DATA,
            ThemeColors.VARIABLE_GROUP,
            ThemeColors.VARIABLE_ENVIRONMENT,
            ThemeColors.VARIABLE_GLOBAL,
            ThemeColors.VARIABLE_BUILT_IN,
            ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND,
            ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND,
            ThemeColors.SPLASH_GRADIENT_START,
            ThemeColors.SPLASH_GRADIENT_END,
            ThemeColors.WINDOW_CHROME_BACKGROUND,
            ThemeColors.DIALOG_CHROME_BACKGROUND,
            ThemeColors.TAB_BACKGROUND,
            ThemeColors.TAB_SELECTED_BACKGROUND,
            ThemeColors.TAB_HOVER_BACKGROUND,
            ThemeColors.TAB_SEPARATOR,
            "Table.background",
            "TableHeader.background",
            "Table.gridColor",
            "Table.selectionBackground",
            "Table.selectionForeground"
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
    public void tearDown() {
        for (Map.Entry<String, Object> entry : previousThemeTokens.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void shouldReadBrandAndStatusColorsFromUiDefaults() {
        Color primary = new Color(1, 2, 3);
        Color success = new Color(4, 5, 6);

        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.SUCCESS, success);

        assertEquals(ModernColors.getPrimary(), primary);
        assertEquals(ModernColors.getSuccess(), success);
    }

    @Test
    public void shouldCreateAlphaColorsFromCurrentThemeTokens() {
        UIManager.put(ThemeColors.PRIMARY, new Color(10, 20, 30));

        assertEquals(ModernColors.primaryWithAlpha(77), new Color(10, 20, 30, 77));
    }

    @Test
    public void shouldApplyAlphaToAnyColor() {
        assertEquals(ModernColors.withAlpha(new Color(1, 2, 3), 44), new Color(1, 2, 3, 44));
    }

    @Test
    public void shouldFormatThemeColorsForHtmlSnippets() {
        assertEquals(ModernColors.toHtmlColor(new Color(1, 35, 255)), "#0123ff");
    }

    @Test
    public void shouldReadSelectionAndSearchHighlightColorsFromUiDefaults() {
        Color selection = new Color(21, 22, 23);
        Color highlight = new Color(31, 32, 33);
        Color currentHighlight = new Color(41, 42, 43);
        UIManager.put(ThemeColors.SELECTION_BACKGROUND, selection);
        UIManager.put(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, highlight);
        UIManager.put(ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND, currentHighlight);

        assertEquals(ModernColors.getSelectionBackgroundColor(), selection);
        assertEquals(ModernColors.getSearchHighlightBackgroundColor(), highlight);
        assertEquals(ModernColors.getSearchCurrentHighlightBackgroundColor(), currentHighlight);
    }

    @Test
    public void shouldReadGitOperationColorsFromUiDefaults() {
        Color commit = new Color(51, 52, 53);
        Color push = new Color(61, 62, 63);
        Color pull = new Color(71, 72, 73);
        UIManager.put(ThemeColors.GIT_COMMIT, commit);
        UIManager.put(ThemeColors.GIT_PUSH, push);
        UIManager.put(ThemeColors.GIT_PULL, pull);

        assertEquals(ModernColors.getGitCommit(), commit);
        assertEquals(ModernColors.getGitPush(), push);
        assertEquals(ModernColors.getGitPull(), pull);
    }

    @Test
    public void shouldReadVariableTypeColorsFromUiDefaults() {
        Color context = new Color(81, 82, 83);
        Color iterationData = new Color(91, 92, 93);
        Color group = new Color(101, 102, 103);
        Color environment = new Color(111, 112, 113);
        Color global = new Color(121, 122, 123);
        Color builtIn = new Color(131, 132, 133);
        UIManager.put(ThemeColors.VARIABLE_CONTEXT, context);
        UIManager.put(ThemeColors.VARIABLE_ITERATION_DATA, iterationData);
        UIManager.put(ThemeColors.VARIABLE_GROUP, group);
        UIManager.put(ThemeColors.VARIABLE_ENVIRONMENT, environment);
        UIManager.put(ThemeColors.VARIABLE_GLOBAL, global);
        UIManager.put(ThemeColors.VARIABLE_BUILT_IN, builtIn);

        assertEquals(ModernColors.getVariableContextColor(), context);
        assertEquals(ModernColors.getVariableIterationDataColor(), iterationData);
        assertEquals(ModernColors.getVariableGroupColor(), group);
        assertEquals(ModernColors.getVariableEnvironmentColor(), environment);
        assertEquals(ModernColors.getVariableGlobalColor(), global);
        assertEquals(ModernColors.getVariableBuiltInColor(), builtIn);
    }

    @Test
    public void shouldReadSplashGradientColorsFromUiDefaults() {
        Color start = new Color(51, 52, 53);
        Color end = new Color(61, 62, 63);
        UIManager.put(ThemeColors.SPLASH_GRADIENT_START, start);
        UIManager.put(ThemeColors.SPLASH_GRADIENT_END, end);

        assertEquals(ModernColors.getSplashGradientStartColor(), start);
        assertEquals(ModernColors.getSplashGradientEndColor(), end);
    }

    @Test
    public void shouldReadTabChromeColorsFromUiDefaults() {
        Color background = new Color(255, 255, 255);
        Color selected = new Color(226, 235, 254);
        Color hover = new Color(242, 246, 255);
        Color separator = new Color(233, 234, 238);
        UIManager.put(ThemeColors.TAB_BACKGROUND, background);
        UIManager.put(ThemeColors.TAB_SELECTED_BACKGROUND, selected);
        UIManager.put(ThemeColors.TAB_HOVER_BACKGROUND, hover);
        UIManager.put(ThemeColors.TAB_SEPARATOR, separator);

        assertEquals(ModernColors.getTabBackgroundColor(), background);
        assertEquals(ModernColors.getTabSelectedBackgroundColor(), selected);
        assertEquals(ModernColors.getTabHoverBackgroundColor(), hover);
        assertEquals(ModernColors.getTabSeparatorColor(), separator);
    }

    @Test
    public void shouldReadWindowAndDialogChromeColorsFromSeparateUiDefaults() {
        Color windowChrome = new Color(233, 234, 238);
        Color dialogChrome = new Color(247, 248, 249);
        UIManager.put(ThemeColors.WINDOW_CHROME_BACKGROUND, windowChrome);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, dialogChrome);

        assertEquals(ModernColors.getWindowChromeBackgroundColor(), windowChrome);
        assertEquals(ModernColors.getDialogChromeBackgroundColor(), dialogChrome);
    }

    @Test
    public void shouldReadTableColorsFromFlatLafUiDefaults() {
        Color background = new Color(255, 255, 255);
        Color header = new Color(244, 246, 248);
        Color grid = new Color(232, 235, 239);
        Color selection = new Color(226, 235, 254);
        Color selectionText = new Color(15, 23, 42);
        UIManager.put("Table.background", background);
        UIManager.put("TableHeader.background", header);
        UIManager.put("Table.gridColor", grid);
        UIManager.put("Table.selectionBackground", selection);
        UIManager.put("Table.selectionForeground", selectionText);

        assertEquals(ModernColors.getTableBackgroundColor(), background);
        assertEquals(ModernColors.getTableHeaderBackgroundColor(), header);
        assertEquals(ModernColors.getTableGridColor(), grid);
        assertEquals(ModernColors.getTableSelectionBackgroundColor(), selection);
        assertEquals(ModernColors.getTableSelectionForegroundColor(), selectionText);
    }
}
