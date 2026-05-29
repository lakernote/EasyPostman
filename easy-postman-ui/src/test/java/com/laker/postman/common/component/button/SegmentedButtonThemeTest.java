package com.laker.postman.common.component.button;

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

public class SegmentedButtonThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.HOVER_BACKGROUND, ThemeColors.BORDER_MEDIUM);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void shouldUseSemanticColorsForSegmentContainer() {
        Color background = new Color(11, 22, 33);
        Color border = new Color(44, 55, 66);
        UIManager.put(ThemeColors.HOVER_BACKGROUND, background);
        UIManager.put(ThemeColors.BORDER_MEDIUM, border);

        assertEquals(SegmentedButtonTheme.segmentBackground(), background);
        assertEquals(SegmentedButtonTheme.segmentBorder(), border);
    }
}
