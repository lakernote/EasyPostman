package com.laker.postman.common.component.button;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class PrimaryButtonThemeTest {

    private static final String BUTTON_DISABLED_BACKGROUND = "EasyPostman.button.disabled.background";
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(BUTTON_DISABLED_BACKGROUND);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void disabledBackgroundShouldUseSemanticButtonToken() throws Exception {
        Color disabledBackground = new Color(31, 32, 33);
        UIManager.put(BUTTON_DISABLED_BACKGROUND, disabledBackground);

        assertEquals(PrimaryButtonTheme.disabledBackground(), disabledBackground);
    }
}
