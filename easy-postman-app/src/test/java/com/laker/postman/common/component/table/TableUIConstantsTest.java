package com.laker.postman.common.component.table;

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

public class TableUIConstantsTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.BORDER_LIGHT,
                ThemeColors.HOVER_BACKGROUND,
                ThemeColors.PRIMARY_LIGHT,
                ThemeColors.TEXT_HINT
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void shouldUseSemanticThemeColors() {
        Color border = new Color(1, 2, 3);
        Color hover = new Color(4, 5, 6);
        Color selectedText = new Color(7, 8, 9);
        Color emptyText = new Color(10, 11, 12);

        UIManager.put(ThemeColors.BORDER_LIGHT, border);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, hover);
        UIManager.put(ThemeColors.PRIMARY_LIGHT, selectedText);
        UIManager.put(ThemeColors.TEXT_HINT, emptyText);

        assertEquals(TableUIConstants.getBorderColor(), border);
        assertEquals(TableUIConstants.getHoverColor(), hover);
        assertEquals(TableUIConstants.getFileSelectedTextColor(), selectedText);
        assertEquals(TableUIConstants.getFileEmptyTextColor(), emptyText);
    }
}
