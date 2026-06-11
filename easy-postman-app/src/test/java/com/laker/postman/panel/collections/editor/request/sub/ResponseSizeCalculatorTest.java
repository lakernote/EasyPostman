package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ResponseSizeCalculatorTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.TEXT_SECONDARY,
                ThemeColors.PRIMARY,
                ThemeColors.SUCCESS
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void compressedResponseSizeShouldUseNeutralMetricColor() {
        Color metricColor = new Color(52, 68, 88);
        Color hoverColor = new Color(42, 101, 211);
        Color successColor = new Color(31, 132, 33);
        UIManager.put(ThemeColors.TEXT_SECONDARY, metricColor);
        UIManager.put(ThemeColors.PRIMARY, hoverColor);
        UIManager.put(ThemeColors.SUCCESS, successColor);

        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setBodyBytesReceived(256);

        ResponseSizeCalculator.SizeInfo sizeInfo = ResponseSizeCalculator.calculate(1024, eventInfo, "gzip");

        assertTrue(sizeInfo.isCompressed());
        assertEquals(sizeInfo.getDisplayText(), "256 B (gzip 75%)");
        assertEquals(sizeInfo.getNormalColor(), ModernColors.getTextSecondary());
        assertEquals(sizeInfo.getHoverColor(), ModernColors.getPrimary());
    }
}
