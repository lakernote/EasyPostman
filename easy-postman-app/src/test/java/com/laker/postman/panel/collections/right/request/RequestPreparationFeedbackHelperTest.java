package com.laker.postman.panel.collections.right.request;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class RequestPreparationFeedbackHelperTest extends AbstractSwingUiTest {

    @Test(description = "清理 URL 校验反馈时只应清除 outline，不应改动 tooltip")
    public void testClearUrlValidationFeedback_ShouldOnlyClearOutline() {
        RequestPreparationFeedbackHelper helper = new RequestPreparationFeedbackHelper();
        JTextField urlField = new JTextField("{{test}}");
        urlField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        urlField.setToolTipText("keep me");

        helper.clearUrlValidationFeedback(urlField);

        assertNull(urlField.getClientProperty(FlatClientProperties.OUTLINE));
        assertEquals(urlField.getToolTipText(), "keep me");
    }
}
