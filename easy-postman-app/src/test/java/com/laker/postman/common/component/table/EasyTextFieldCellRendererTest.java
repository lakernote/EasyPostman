package com.laker.postman.common.component.table;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class EasyTextFieldCellRendererTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.TEXT_PRIMARY);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void stripeBackgroundShouldBlendWithThemeTextColor() {
        Color base = new Color(200, 204, 208);
        Color textPrimary = new Color(20, 40, 60);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);

        assertEquals(EasyTextFieldCellRenderer.stripeBackground(base), blend(base, textPrimary, 0.04f));
    }

    private static Color blend(Color base, Color blend, float alpha) {
        int red = Math.min(255, Math.max(0, Math.round(base.getRed() * (1 - alpha) + blend.getRed() * alpha)));
        int green = Math.min(255, Math.max(0, Math.round(base.getGreen() * (1 - alpha) + blend.getGreen() * alpha)));
        int blue = Math.min(255, Math.max(0, Math.round(base.getBlue() * (1 - alpha) + blend.getBlue() * alpha)));
        return new Color(red, green, blue);
    }
}
