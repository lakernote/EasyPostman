package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class WebSocketStagePropertyPanelTest {

    @Test
    public void shouldResolveAwaitModeHintForEveryCompletionMode() {
        for (WebSocketPerformanceData.CompletionMode mode : WebSocketPerformanceData.CompletionMode.values()) {
            String hint = WebSocketStagePropertyPanel.resolveAwaitModeHint(mode);

            assertFalse(hint == null || hint.isBlank(), mode.name());
        }
    }

    @Test
    public void shouldUseEasyComboBoxForAwaitMode() throws Exception {
        Field field = WebSocketStagePropertyPanel.class.getDeclaredField("completionModeBox");

        assertEquals(field.getType(), EasyComboBox.class);
    }

    @Test
    public void shouldUseCompactWebSocketSendLabels() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_NONE), "不发送");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY), "发送");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT), "重复发送");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_MESSAGE_TEMPLATE), "Content");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_PRE_SCRIPT), "Pre-script");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_NONE), "No Send");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY), "Send");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT), "Repeat");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_MESSAGE_TEMPLATE), "Content");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_PRE_SCRIPT), "Pre-script");
    }
}
