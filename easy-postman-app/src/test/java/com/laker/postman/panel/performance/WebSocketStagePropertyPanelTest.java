package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

public class WebSocketStagePropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldResolveReadModeHintForEveryCompletionMode() {
        for (WebSocketPerformanceData.CompletionMode mode : WebSocketPerformanceData.CompletionMode.values()) {
            String hint = WebSocketStagePropertyPanel.resolveReadModeHint(mode);

            assertFalse(hint == null || hint.isBlank(), mode.name());
            assertFalse(!hint.contains("消息正文")
                    && !hint.toLowerCase(Locale.ROOT).contains("message bodies"), mode.name());
        }
    }

    @Test
    public void shouldUseEasyComboBoxForReadMode() throws Exception {
        Field field = WebSocketStagePropertyPanel.class.getDeclaredField("completionModeBox");

        assertEquals(field.getType(), EasyComboBox.class);
    }

    @Test
    public void nonSendStagesShouldNotCreateSendEditorsBeforeSelection() throws Exception {
        WebSocketStagePropertyPanel connectPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CONNECT);
        WebSocketStagePropertyPanel readPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.READ);
        WebSocketStagePropertyPanel closePanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CLOSE);

        assertNull(fieldValue(connectPanel, "customSendBodyArea", RSyntaxTextArea.class));
        assertNull(fieldValue(readPanel, "customSendBodyArea", RSyntaxTextArea.class));
        assertNull(fieldValue(closePanel, "customSendBodyArea", RSyntaxTextArea.class));
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
    public void shouldDescribeReadAsNonClosingStep() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_MENU_ADD_WS_READ), "添加 WS Read");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_NODE_READ), "WS Read");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_READ_MODE), "读取方式");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE), "读 1 条");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE), "读到包含文本");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT), "读 N 条");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION), "观察一段时间");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_MESSAGE_FILTER), "包含文本");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_MEMORY),
                "消息正文仅在过滤、断言或提取器需要时保留。");
        String firstMessageHint = zh.getString(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE);
        assertEquals(firstMessageHint, "读到 1 条即继续；超时失败。");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_MATCHED_MESSAGE),
                "读到包含文本即继续；这是过滤，不是断言。");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_FIXED_DURATION),
                "观察到设定时长即继续；提前断开失败。");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_MESSAGE_COUNT),
                "达到目标数量即继续；超时未达标失败。");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_CLOSE_HINT),
                "执行到该步骤时主动关闭当前 WebSocket 连接；Read 步骤只负责读取消息，不负责关闭连接。");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_MENU_ADD_WS_READ), "Add WS Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_NODE_READ), "WS Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_READ_MODE), "Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE), "1 message");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE), "Until contains");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT), "N messages");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION), "Observe duration");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_MESSAGE_FILTER), "Contains");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_MEMORY),
                "Message bodies are retained only for filters, assertions, or extractors.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE),
                "Continue after 1 message; timeout fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_MATCHED_MESSAGE),
                "Continue when a message contains text; this filters, not asserts.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_FIXED_DURATION),
                "Continue after the duration; early close fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_MESSAGE_COUNT),
                "Continue at target count; timeout before target fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_CLOSE_HINT),
                "When this step is reached, the current WebSocket connection is actively closed. Read steps only receive messages; they do not close the connection.");
    }

    private static <T> T fieldValue(Object owner, String fieldName, Class<T> type) throws Exception {
        Field field = WebSocketStagePropertyPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }
}
