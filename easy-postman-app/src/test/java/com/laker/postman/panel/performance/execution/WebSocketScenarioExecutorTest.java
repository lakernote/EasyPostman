package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataVariableService;
import com.laker.postman.service.variable.VariablesService;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WebSocketScenarioExecutorTest {

    @Test
    public void shouldBuildResponseBodyFromAllReceivedMessages() {
        String body = WebSocketScenarioExecutor.buildResponseBody(Arrays.asList("first", "second"));

        assertEquals(body, "first\n\nsecond\n\n");
    }

    @Test
    public void shouldReturnEmptyBodyWhenNoMessagesReceived() {
        String body = WebSocketScenarioExecutor.buildResponseBody(Collections.emptyList());

        assertEquals(body, "");
    }

    @Test
    public void shouldResolveCustomSendBodyWithExecutionAndIterationVariables() throws Exception {
        VariablesService.getInstance().detachContext();
        IterationDataVariableService.getInstance().detachContext();

        CountDownLatch serverReceivedMessage = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>("");

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    receivedPayload.set(text);
                    serverReceivedMessage.countDown();
                    webSocket.send("ack:" + text);
                    webSocket.close(1000, "done");
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    serverReceivedMessage.countDown();
                }
            }));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("ws-custom-variable-test");
            item.setName("WS Custom Variable Test");
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            item.setMethod("GET");
            item.setUrl(server.url("/socket").toString().replaceFirst("^http", "ws"));
            item.setPrescript("pm.variables.set('runToken', 'script-token');");

            WebSocketPerformanceData requestCfg = new WebSocketPerformanceData();
            requestCfg.connectTimeoutMs = 2000;

            JMeterTreeNode requestData = new JMeterTreeNode("request", NodeType.REQUEST, item);
            requestData.webSocketPerformanceData = requestCfg;
            DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);

            JMeterTreeNode sendStep = new JMeterTreeNode("send", NodeType.WS_SEND);
            sendStep.webSocketPerformanceData = new WebSocketPerformanceData();
            sendStep.webSocketPerformanceData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT;
            sendStep.webSocketPerformanceData.sendContentSource = WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT;
            sendStep.webSocketPerformanceData.customSendBody = "{{runToken}}/{{csvUser}}";
            requestNode.add(new DefaultMutableTreeNode(sendStep));

            JMeterTreeNode awaitStep = new JMeterTreeNode("await", NodeType.WS_AWAIT);
            awaitStep.webSocketPerformanceData = new WebSocketPerformanceData();
            awaitStep.webSocketPerformanceData.completionMode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
            awaitStep.webSocketPerformanceData.firstMessageTimeoutMs = 2000;
            requestNode.add(new DefaultMutableTreeNode(awaitStep));

            ExecutionVariableContext iterationContext = new ExecutionVariableContext(
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(Map.of("csvUser", "csv-alice"))
            );

            PerformanceRequestExecutor executor = new PerformanceRequestExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    ConcurrentHashMap.newKeySet()
            );

            executor.execute(requestNode, requestData, iterationContext);

            assertTrue(serverReceivedMessage.await(1, TimeUnit.SECONDS), "WebSocket server should receive one message");
            assertEquals(receivedPayload.get(), "script-token/csv-alice");
        } finally {
            VariablesService.getInstance().detachContext();
            IterationDataVariableService.getInstance().detachContext();
        }
    }

    @Test
    public void shouldRunWebSocketSendPreScriptBeforeEveryRepeatedCustomPayload() throws Exception {
        VariablesService.getInstance().detachContext();
        IterationDataVariableService.getInstance().detachContext();

        CountDownLatch serverReceivedMessages = new CountDownLatch(3);
        List<String> receivedPayloads = new CopyOnWriteArrayList<>();

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    receivedPayloads.add(text);
                    serverReceivedMessages.countDown();
                    if (serverReceivedMessages.getCount() == 0) {
                        webSocket.close(1000, "done");
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    while (serverReceivedMessages.getCount() > 0) {
                        serverReceivedMessages.countDown();
                    }
                }
            }));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("ws-repeated-custom-script-test");
            item.setName("WS Repeated Custom Script Test");
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            item.setMethod("GET");
            item.setUrl(server.url("/socket").toString().replaceFirst("^http", "ws"));
            item.setPrescript("pm.variables.set('prefix', 'script');");

            WebSocketPerformanceData requestCfg = new WebSocketPerformanceData();
            requestCfg.connectTimeoutMs = 2000;

            JMeterTreeNode requestData = new JMeterTreeNode("request", NodeType.REQUEST, item);
            requestData.webSocketPerformanceData = requestCfg;
            DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);

            JMeterTreeNode sendStep = new JMeterTreeNode("send", NodeType.WS_SEND);
            sendStep.webSocketPerformanceData = new WebSocketPerformanceData();
            sendStep.webSocketPerformanceData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
            sendStep.webSocketPerformanceData.sendContentSource = WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT;
            sendStep.webSocketPerformanceData.sendCount = 3;
            sendStep.webSocketPerformanceData.sendIntervalMs = 0;
            sendStep.webSocketPerformanceData.sendPreScript = """
                    pm.variables.set('a', pm.variables.get('prefix') + '-' + pm.info.wsSendIndex);
                    """;
            sendStep.webSocketPerformanceData.customSendBody = "{{a}}/{{csvUser}}";
            requestNode.add(new DefaultMutableTreeNode(sendStep));

            ExecutionVariableContext iterationContext = new ExecutionVariableContext(
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(Map.of("csvUser", "csv-alice"))
            );

            PerformanceRequestExecutor executor = new PerformanceRequestExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    ConcurrentHashMap.newKeySet()
            );

            executor.execute(requestNode, requestData, iterationContext);

            assertTrue(serverReceivedMessages.await(1, TimeUnit.SECONDS), "WebSocket server should receive repeated messages");
            assertEquals(receivedPayloads, List.of("script-0/csv-alice", "script-1/csv-alice", "script-2/csv-alice"));
        } finally {
            VariablesService.getInstance().detachContext();
            IterationDataVariableService.getInstance().detachContext();
        }
    }

    @Test
    public void shouldResolveOriginalRequestBodyTemplateForEveryRepeatedWebSocketSend() throws Exception {
        VariablesService.getInstance().detachContext();
        IterationDataVariableService.getInstance().detachContext();

        CountDownLatch serverReceivedMessages = new CountDownLatch(2);
        List<String> receivedPayloads = new CopyOnWriteArrayList<>();

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    receivedPayloads.add(text);
                    serverReceivedMessages.countDown();
                    if (serverReceivedMessages.getCount() == 0) {
                        webSocket.close(1000, "done");
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    while (serverReceivedMessages.getCount() > 0) {
                        serverReceivedMessages.countDown();
                    }
                }
            }));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("ws-repeated-body-template-test");
            item.setName("WS Repeated Body Template Test");
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            item.setMethod("GET");
            item.setUrl(server.url("/socket").toString().replaceFirst("^http", "ws"));
            item.setBody("{{a}}");
            item.setPrescript("pm.variables.set('a', 'connect-value');");

            WebSocketPerformanceData requestCfg = new WebSocketPerformanceData();
            requestCfg.connectTimeoutMs = 2000;

            JMeterTreeNode requestData = new JMeterTreeNode("request", NodeType.REQUEST, item);
            requestData.webSocketPerformanceData = requestCfg;
            DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);

            JMeterTreeNode sendStep = new JMeterTreeNode("send", NodeType.WS_SEND);
            sendStep.webSocketPerformanceData = new WebSocketPerformanceData();
            sendStep.webSocketPerformanceData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
            sendStep.webSocketPerformanceData.sendContentSource = WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
            sendStep.webSocketPerformanceData.sendCount = 2;
            sendStep.webSocketPerformanceData.sendIntervalMs = 0;
            sendStep.webSocketPerformanceData.sendPreScript = """
                    pm.variables.set('a', 'body-' + pm.info.wsSendIndex);
                    """;
            requestNode.add(new DefaultMutableTreeNode(sendStep));

            PerformanceRequestExecutor executor = new PerformanceRequestExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    ConcurrentHashMap.newKeySet()
            );

            executor.execute(requestNode, requestData, new ExecutionVariableContext());

            assertTrue(serverReceivedMessages.await(1, TimeUnit.SECONDS), "WebSocket server should receive repeated body messages");
            assertEquals(receivedPayloads, List.of("body-0", "body-1"));
        } finally {
            VariablesService.getInstance().detachContext();
            IterationDataVariableService.getInstance().detachContext();
        }
    }
}
