package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.util.I18nUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ResponseTabBadgeControllerTest {
    private boolean originalChinese;

    @BeforeMethod
    public void useEnglishLabels() {
        originalChinese = I18nUtil.isChinese();
        I18nUtil.setLocale("en");
    }

    @AfterMethod
    public void restoreLocale() {
        I18nUtil.setLocale(originalChinese ? "zh" : "en");
    }

    @Test
    public void headersCountShouldUseNeutralMetadataColor() {
        JButton headersButton = new JButton();
        ResponseTabBadgeController controller = new ResponseTabBadgeController(new JButton[]{
                new JButton(),
                headersButton,
                new JButton()
        });

        controller.updateResponseHeadersCount(6);

        String text = headersButton.getText();
        assertTrue(text.contains("Response Headers"));
        assertTrue(text.contains("&middot; 6"));
        assertTrue(text.contains(ModernColors.toHtmlColor(ModernColors.getTextSecondary())));
        assertFalse(text.contains(ModernColors.toHtmlColor(ModernColors.getSuccess())),
                "Header count is metadata, not success state");
    }

    @Test
    public void testsCountShouldKeepResultSemanticColor() {
        JButton testsButton = new JButton();
        ResponseTabBadgeController controller = new ResponseTabBadgeController(new JButton[]{
                new JButton(),
                new JButton(),
                testsButton
        });

        controller.updateTestResults(List.of(new TestResult("status", false, "boom")));

        String text = testsButton.getText();
        assertTrue(text.contains("Tests"));
        assertTrue(text.contains("&middot; 1"));
        assertTrue(text.contains(ModernColors.toHtmlColor(ModernColors.getError())));
    }

    @Test
    public void labelsShouldResetWithoutCounts() {
        JButton headersButton = new JButton();
        JButton testsButton = new JButton();
        ResponseTabBadgeController controller = new ResponseTabBadgeController(new JButton[]{
                new JButton(),
                headersButton,
                testsButton
        });

        controller.updateResponseHeadersCount(0);
        controller.updateTestResults(List.of());

        assertFalse(headersButton.getText().contains("&middot;"));
        assertFalse(testsButton.getText().contains("&middot;"));
    }
}
