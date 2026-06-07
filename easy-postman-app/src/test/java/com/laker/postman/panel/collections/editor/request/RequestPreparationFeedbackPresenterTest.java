package com.laker.postman.panel.collections.editor.request;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class RequestPreparationFeedbackPresenterTest extends AbstractSwingUiTest {

    @Test(description = "清理 URL 校验反馈时只应清除 outline，不应改动 tooltip")
    public void testClearUrlValidationFeedback_ShouldOnlyClearOutline() {
        RequestPreparationFeedbackPresenter presenter = new RequestPreparationFeedbackPresenter();
        JTextField urlField = new JTextField("{{test}}");
        urlField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        urlField.setToolTipText("keep me");

        presenter.clearUrlValidationFeedback(urlField);

        assertNull(urlField.getClientProperty(FlatClientProperties.OUTLINE));
        assertEquals(urlField.getToolTipText(), "keep me");
    }
}
