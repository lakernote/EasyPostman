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

    @Test
    public void shouldDescribeAwaitAsNonClosingStep() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_AWAIT_MODE), "等待完成条件");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE), "收到任意消息");
        String firstMessageHint = zh.getString(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE);
        assertEquals(firstMessageHint,
                "等待收到任意 WebSocket 消息后进入下一步；不会主动关闭连接，超过超时时间仍未收到则失败。");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_CLOSE_HINT),
                "执行到该步骤时主动关闭当前 WebSocket 连接；Await 步骤只负责等待条件，不负责关闭连接。");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_AWAIT_MODE), "Await Completion");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE), "Any Message");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE),
                "Wait for any WebSocket message, then continue to the next step. This does not close the connection. If no message arrives before the timeout, the step fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_CLOSE_HINT),
                "When this step is reached, the current WebSocket connection is actively closed. Await steps only wait for completion conditions; they do not close the connection.");
    }
}
