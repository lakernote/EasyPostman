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

public class AuthTabPanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.ACCENT,
                ThemeColors.TEXT_HINT,
                ThemeColors.TEXT_SECONDARY
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void htmlTextColorsShouldUseSemanticThemeTokens() {
        UIManager.put(ThemeColors.ACCENT, new Color(1, 2, 3));
        UIManager.put(ThemeColors.TEXT_HINT, new Color(11, 12, 13));
        UIManager.put(ThemeColors.TEXT_SECONDARY, new Color(21, 22, 23));

        assertEquals(AuthTabTheme.titleColorHex(), "#010203");
        assertEquals(AuthTabTheme.descriptionColorHex(), "#0b0c0d");
        assertEquals(AuthTabTheme.textColorHex(), "#151617");
    }
}
