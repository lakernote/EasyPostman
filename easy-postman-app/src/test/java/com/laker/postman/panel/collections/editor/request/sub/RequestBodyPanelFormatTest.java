package com.laker.postman.panel.collections.editor.request.sub;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RequestBodyPanelFormatTest {

    @Test(description = "JSON 格式化遇到多个顶层对象时不抛异常")
    public void jsonFormatReturnsEmptyWhenInputHasTrailingTokens() {
        Optional<String> formatted = RequestBodyPanel.formatBodyTextForDisplay(
                RequestBodyPanel.RAW_TYPE_JSON, "{\"a\":1}{\"b\":2}");

        assertTrue(formatted.isEmpty());
    }

    @Test(description = "JSON 格式化对有效对象返回格式化文本")
    public void jsonFormatReturnsPrettyTextForValidObject() {
        Optional<String> formatted = RequestBodyPanel.formatBodyTextForDisplay(
                RequestBodyPanel.RAW_TYPE_JSON, "{\"a\":1}");

        assertTrue(formatted.isPresent());
        assertEquals(formatted.get(), """
                {
                    "a": 1
                }""");
    }
}
