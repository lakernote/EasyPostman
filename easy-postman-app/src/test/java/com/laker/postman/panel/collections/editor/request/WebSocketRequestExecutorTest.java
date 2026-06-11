package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.component.button.WebSocketSendButton;
import com.laker.postman.common.component.button.WebSocketTimedSendButton;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import okhttp3.WebSocketListener;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class WebSocketRequestExecutorTest {

    @Test(description = "WebSocket 发送应打开握手请求快照和网络日志，和 HTTP/SSE 保持一致")
    public void shouldEnableNetworkCaptureForWebSocketRequests() {
        PreparedRequest request = new PreparedRequest();

        new WebSocketRequestExecutor(
                null,
                null,
                null,
                null,
                new RequestExecutionState()
        ).createWorker(request, null);

        assertTrue(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertTrue(request.collectEventInfo);
        assertTrue(request.enableNetworkLog);
    }

    @Test(description = "远端发起关闭时应结束 WebSocket 会话并把顶部按钮恢复为连接")
    public void shouldResetConnectButtonWhenRemotePeerStartsClosing() throws Exception {
        RequestExecutionState requestState = new RequestExecutionState();
        RequestLinePanel requestLinePanel = newHeadlessRequestLinePanel();
        ResponsePanel responsePanel = new ResponsePanel(RequestItemProtocolEnum.WEBSOCKET, false);
        RequestBodyPanel requestBodyPanel = newHeadlessRequestBodyPanel();
        JTabbedPane requestTabs = new JTabbedPane();
        RequestExecutionUiUpdater uiUpdater = new RequestExecutionUiUpdater(
                responsePanel,
                requestLinePanel,
                requestBodyPanel,
                requestTabs,
                e -> {
                },
                () -> false,
                () -> false,
                () -> false,
                () -> true
        );
        WebSocketRequestExecutor executor = new WebSocketRequestExecutor(
                responsePanel,
                uiUpdater,
                new RequestStreamUiAppender(responsePanel, DateTimeFormatter.ofPattern("HH:mm:ss")),
                new RequestResponseHandler(new JPanel(), responsePanel, results -> {
                }, (request, response) -> {
                }),
                requestState
        );

        Object session = newSession(executor);
        String connectionId = (String) invoke(session, "connectionId");
        requestState.beginWebSocketConnection(connectionId);
        requestState.attachWebSocketConnection(new NoopWebSocketConnection());
        uiUpdater.switchSendButtonToClose();

        WebSocketListener listener = (WebSocketListener) invoke(session, "newListener");
        listener.onClosing(null, 1013, "session replacement failed");
        flushEdt();

        WebSocketExecutionState sessionState = (WebSocketExecutionState) readField(session, "executionState");
        assertTrue(sessionState.shouldSaveHistory(false));
        assertNull(requestState.currentWebSocket());
        assertFalse(requestBodyPanel.getWsSendButton().isEnabled());
        assertEquals(requestLinePanel.getSendButton().getText(), I18nUtil.getMessage(MessageKeys.BUTTON_CONNECT));
    }

    private Object newSession(WebSocketRequestExecutor executor) throws Exception {
        Class<?> sessionClass = Class.forName(WebSocketRequestExecutor.class.getName() + "$WebSocketSession");
        Constructor<?> constructor = sessionClass.getDeclaredConstructor(
                WebSocketRequestExecutor.class,
                PreparedRequest.class,
                com.laker.postman.service.js.ScriptExecutionPipeline.class
        );
        constructor.setAccessible(true);
        PreparedRequest request = new PreparedRequest();
        request.url = "ws://example.test/socket";
        request.method = "GET";
        return constructor.newInstance(executor, request, null);
    }

    private Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private RequestLinePanel newHeadlessRequestLinePanel() throws Exception {
        RequestLinePanel requestLinePanel = allocateWithoutConstructor(RequestLinePanel.class);
        writeField(requestLinePanel, "protocol", RequestItemProtocolEnum.WEBSOCKET);
        writeField(requestLinePanel, "sendButton", new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLOSE)));
        return requestLinePanel;
    }

    private RequestBodyPanel newHeadlessRequestBodyPanel() throws Exception {
        RequestBodyPanel requestBodyPanel = allocateWithoutConstructor(RequestBodyPanel.class);
        writeField(requestBodyPanel, "wsSendButton", new WebSocketSendButton());
        writeField(requestBodyPanel, "wsTimedSendButton", new WebSocketTimedSendButton());
        return requestBodyPanel;
    }

    private <T> T allocateWithoutConstructor(Class<T> type) throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        return type.cast(unsafe.allocateInstance(type));
    }

    private void writeField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void flushEdt() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private static final class NoopWebSocketConnection implements com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection {
        @Override
        public boolean send(String text) {
            return true;
        }

        @Override
        public long queueSize() {
            return 0;
        }

        @Override
        public boolean close(int code, String reason) {
            return true;
        }

        @Override
        public void cancel() {
        }

        @Override
        public Object metricKey() {
            return this;
        }
    }
}
