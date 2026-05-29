package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;
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

public class RequestEditorPlaceholderThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.BORDER_LIGHT,
                ThemeColors.HOVER_BACKGROUND,
                ThemeColors.PRIMARY
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void skeletonColorsShouldUseSemanticThemeTokens() throws Exception {
        Color block = new Color(41, 42, 43);
        Color softBlock = new Color(51, 52, 53);
        Color primary = new Color(61, 62, 63);
        UIManager.put(ThemeColors.BORDER_LIGHT, block);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, softBlock);
        UIManager.put(ThemeColors.PRIMARY, primary);

        assertEquals(PlaceholderTheme.skeletonBlock(), block);
        assertEquals(PlaceholderTheme.skeletonSoftBlock(), softBlock);
        assertEquals(PlaceholderTheme.skeletonAccent(), ModernColors.withAlpha(primary, 64));
        assertEquals(PlaceholderTheme.skeletonAccentLine(), ModernColors.withAlpha(primary, 96));
    }
}
