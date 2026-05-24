package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.service.setting.SettingManager;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceRequestExecutorTest {

    @Test
    public void shouldUseFullResponseBodyOutsideEfficientMode() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyModeForAssertionElements(
                false,
                List.of(),
                ""
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.FULL);
    }

    @Test
    public void shouldSkipResponseBodyInEfficientModeWhenOnlyMetadataIsNeeded() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyModeForAssertionElements(
                true,
                List.of(assertionElement("Response Code")),
                ""
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.METADATA_ONLY);
    }

    @Test
    public void shouldUsePreviewInEfficientModeWhenAssertionsNeedBody() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyModeForAssertionElements(
                true,
                List.of(assertionElement("Contains")),
                ""
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.PREVIEW);
    }

    @Test
    public void shouldUsePreviewInEfficientModeWhenPostScriptMayReadBody() {
        PreparedRequest.ResponseBodyMode mode = PerformanceRequestExecutor.resolveHttpResponseBodyModeForAssertionElements(
                true,
                List.of(),
                "pm.test('body', () => pm.response.text())"
        );

        assertEquals(mode, PreparedRequest.ResponseBodyMode.PREVIEW);
    }

    @Test
    public void shouldResolveConfiguredPreviewLimitToBytes() {
        assertEquals(PerformanceRequestExecutor.resolveResponseBodyPreviewLimitBytes(4), 4 * 1024);
    }

    @Test
    public void shouldClampInvalidPreviewLimitToDefault() {
        assertEquals(
                PerformanceRequestExecutor.resolveResponseBodyPreviewLimitBytes(0),
                SettingManager.DEFAULT_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_KB * 1024
        );
    }

    @Test
    public void shouldDisableCookieNotificationsForPerformanceRequests() throws Exception {
        PreparedRequest request = new PreparedRequest();
        PerformanceRequestExecutor executor = new PerformanceRequestExecutor(
                () -> true,
                throwable -> false,
                java.util.concurrent.ConcurrentHashMap.newKeySet(),
                java.util.concurrent.ConcurrentHashMap.newKeySet()
        );
        Method configureMethod = PerformanceRequestExecutor.class.getDeclaredMethod(
                "configurePreparedRequest",
                PreparedRequest.class
        );
        configureMethod.setAccessible(true);

        configureMethod.invoke(executor, request);

        assertFalse(request.notifyCookieChanges);
    }

    @Test(description = "WebSocket 请求缺少启用的 Connect 阶段时不应真实发起连接")
    public void shouldNotOpenWebSocketWhenConnectStageIsMissing() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                    webSocket.close(1000, "unexpected");
                }
            }));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("ws-missing-connect");
            item.setName("WS Missing Connect");
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            item.setMethod("GET");
            item.setUrl(server.url("/socket").toString().replaceFirst("^http", "ws"));
            JMeterTreeNode requestData = new JMeterTreeNode(item.getName(), NodeType.REQUEST, item);
            requestData.webSocketPerformanceData = new WebSocketPerformanceData();
            requestData.webSocketPerformanceData.connectTimeoutMs = 500;
            DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);

            PerformanceRequestExecutionResult result = new PerformanceRequestExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    ConcurrentHashMap.newKeySet()
            ).execute(requestNode, requestData, new com.laker.postman.service.variable.ExecutionVariableContext());

            assertTrue(result.executionFailed);
            assertEquals(server.getRequestCount(), 0);
        }
    }

    @Test(description = "SSE 请求缺少启用的 Connect 或 Receive 阶段时不应真实发起连接")
    public void shouldNotOpenSseWhenRequiredStagesAreMissing() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: unexpected\n\n"));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("sse-missing-connect");
            item.setName("SSE Missing Connect");
            item.setProtocol(RequestItemProtocolEnum.HTTP);
            item.setMethod("GET");
            item.setUrl(server.url("/events").toString());
            item.setHeadersList(List.of(new HttpHeader(true, "Accept", "text/event-stream")));
            JMeterTreeNode requestData = new JMeterTreeNode(item.getName(), NodeType.REQUEST, item);
            requestData.ssePerformanceData = new SsePerformanceData();
            requestData.ssePerformanceData.firstMessageTimeoutMs = 100;
            DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);

            PerformanceRequestExecutionResult result = new PerformanceRequestExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    ConcurrentHashMap.newKeySet()
            ).execute(requestNode, requestData, new com.laker.postman.service.variable.ExecutionVariableContext());

            assertTrue(result.executionFailed);
            assertEquals(server.getRequestCount(), 0);
        }
    }

    private static PerformanceAssertionElement assertionElement(String type) {
        AssertionData data = new AssertionData();
        data.type = type;
        return new PerformanceAssertionElement(type, data);
    }
}
