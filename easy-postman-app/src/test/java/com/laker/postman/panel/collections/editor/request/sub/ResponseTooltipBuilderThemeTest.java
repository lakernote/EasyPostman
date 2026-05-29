package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class ResponseTooltipBuilderThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.SUCCESS);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void compressionBadgeBackgroundShouldUseSemanticSuccessToken() throws Exception {
        Color success = new Color(31, 132, 33);
        UIManager.put(ThemeColors.SUCCESS, success);
        ResponseSizeCalculator.SizeInfo sizeInfo = new ResponseSizeCalculator.SizeInfo(
                "10 KB", success, success, true, 50, 1024, "gzip");

        JPanel wrapper = invokeCompressionBadge(sizeInfo);
        JPanel badge = (JPanel) wrapper.getComponent(0);

        assertEquals(badge.getBackground(), ModernColors.withAlpha(success, 24));
    }

    private JPanel invokeCompressionBadge(ResponseSizeCalculator.SizeInfo sizeInfo) throws Exception {
        Method method = ResponseTooltipBuilder.class.getDeclaredMethod(
                "compressionBadge", ResponseSizeCalculator.SizeInfo.class);
        method.setAccessible(true);
        return (JPanel) method.invoke(null, sizeInfo);
    }
}
