package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.variable.VariableResolver;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class WebSocketScenarioExecutor {
    private static final int MAX_RETAINED_AWAIT_MESSAGES = 1024;

    public static final class Result {
        public final HttpResponse response;
        public final String errorMsg;
        public final boolean executionFailed;
        public final boolean interrupted;
        public final List<TestResult> testResults;

        public Result(HttpResponse response, String errorMsg, boolean executionFailed,
                      boolean interrupted, List<TestResult> testResults) {
            this.response = response;
            this.errorMsg = errorMsg;
            this.executionFailed = executionFailed;
            this.interrupted = interrupted;
            this.testResults = testResults;
        }
    }

    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<WebSocket> activeWebSockets;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final int responseBodyPreviewLimitBytes;

    public WebSocketScenarioExecutor(BooleanSupplier runningSupplier,
                                     Predicate<Throwable> cancelledChecker,
                                     Set<WebSocket> activeWebSockets) {
        this(runningSupplier, cancelledChecker, activeWebSockets, new PerformanceRealtimeMetrics());
    }

    public WebSocketScenarioExecutor(BooleanSupplier runningSupplier,
                                     Predicate<Throwable> cancelledChecker,
                                     Set<WebSocket> activeWebSockets,
                                     PerformanceRealtimeMetrics realtimeMetrics) {
        this(runningSupplier, cancelledChecker, activeWebSockets, realtimeMetrics,
                BoundedTextAccumulator.DEFAULT_PREVIEW_BYTES);
    }

    public WebSocketScenarioExecutor(BooleanSupplier runningSupplier,
                                     Predicate<Throwable> cancelledChecker,
                                     Set<WebSocket> activeWebSockets,
                                     PerformanceRealtimeMetrics realtimeMetrics,
                                     int responseBodyPreviewLimitBytes) {
        this.runningSupplier = runningSupplier;
        this.cancelledChecker = cancelledChecker;
        this.activeWebSockets = activeWebSockets;
        this.realtimeMetrics = realtimeMetrics == null ? new PerformanceRealtimeMetrics() : realtimeMetrics;
        this.responseBodyPreviewLimitBytes = Math.max(1, responseBodyPreviewLimitBytes);
    }

    public Result execute(PreparedRequest req,
                          DefaultMutableTreeNode requestNode,
                          WebSocketPerformanceData requestCfg,
                          String requestBodyTemplate,
                          ScriptExecutionPipeline pipeline) {
        return execute(req, requestNode, requestCfg, requestBodyTemplate, pipeline, "", "");
    }

    public Result execute(PreparedRequest req,
                          DefaultMutableTreeNode requestNode,
                          WebSocketPerformanceData requestCfg,
                          String requestBodyTemplate,
                          ScriptExecutionPipeline pipeline,
                          String apiId,
                          String apiName) {
        long requestStartTime = System.currentTimeMillis();
        HttpResponse resp = new HttpResponse();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean closingSocket = new AtomicBoolean(false);
        AtomicBoolean remoteClosed = new AtomicBoolean(false);
        AtomicReference<String> errorRef = new AtomicReference<>("");
        AtomicReference<String> lastMessageRef = new AtomicReference<>("");
        AtomicReference<WebSocketPerformanceData> lastStepCfgRef = new AtomicReference<>(requestCfg);
        BoundedTextAccumulator responseBody = new BoundedTextAccumulator(responseBodyPreviewLimitBytes);
        AtomicLong sampleEndTimeMs = new AtomicLong(0);
        AtomicLong firstMessageLatencyMs = new AtomicLong(-1);
        AtomicBoolean firstReceivedMessageRecorded = new AtomicBoolean(false);
        AtomicInteger receivedMessageCount = new AtomicInteger(0);
        AtomicInteger matchedMessageCount = new AtomicInteger(0);
        AtomicInteger sentMessageCount = new AtomicInteger(0);
        AtomicBoolean sessionRegistered = new AtomicBoolean(false);
        List<TestResult> stepTestResults = new ArrayList<>();
        Deque<ReceivedWebSocketMessage> receivedMessages = new ArrayDeque<>();
        AtomicLong receivedMessagesRetainedBytes = new AtomicLong(0);
        boolean keepReceivedMessages = hasEnabledAwaitStep(requestNode);
        Object messageLock = new Object();
        CountDownLatch openLatch = new CountDownLatch(1);

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                resp.headers = new LinkedHashMap<>();
                for (String name : response.headers().names()) {
                    resp.addHeader(name, response.headers(name));
                }
                resp.code = response.code();
                resp.protocol = response.protocol().toString();
                if (sessionRegistered.compareAndSet(false, true)) {
                    realtimeMetrics.recordWebSocketSessionStart(webSocket, requestStartTime, apiId, apiName);
                }
                openLatch.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                appendMessage(webSocket, text);
            }

            @Override
            public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
                appendMessage(webSocket, toHexPreview(bytes));
            }

            private void appendMessage(WebSocket webSocket, String payload) {
                String value = payload == null ? "" : payload;
                lastMessageRef.set(headerPreview(value));
                responseBody.append(value);
                responseBody.append("\n\n");
                receivedMessageCount.incrementAndGet();
                long receivedAtMs = System.currentTimeMillis();
                realtimeMetrics.recordWebSocketReceived(webSocket);
                if (firstReceivedMessageRecorded.compareAndSet(false, true)) {
                    long latencyMs = Math.max(0, receivedAtMs - requestStartTime);
                    firstMessageLatencyMs.compareAndSet(-1, latencyMs);
                    realtimeMetrics.recordWebSocketFirstMessageLatency(webSocket, latencyMs);
                }
                synchronized (messageLock) {
                    if (keepReceivedMessages) {
                        addReceivedMessage(
                                receivedMessages,
                                value,
                                receivedAtMs,
                                receivedMessagesRetainedBytes,
                                responseBodyPreviewLimitBytes
                        );
                    }
                    messageLock.notifyAll();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                remoteClosed.set(true);
                openLatch.countDown();
                synchronized (messageLock) {
                    messageLock.notifyAll();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
                if (response != null) {
                    if (resp.headers == null) {
                        resp.headers = new LinkedHashMap<>();
                    }
                    for (String name : response.headers().names()) {
                        resp.addHeader(name, response.headers(name));
                    }
                    resp.code = response.code();
                    resp.protocol = response.protocol().toString();
                }
                String message = throwable != null ? throwable.getMessage() : "";
                if (!closingSocket.get()) {
                    if (!runningSupplier.getAsBoolean() || cancelledChecker.test(throwable)) {
                        interrupted.set(true);
                    } else {
                        failed.set(true);
                        errorRef.set(CharSequenceUtil.blankToDefault(message, "WebSocket request failed"));
                    }
                }
                remoteClosed.set(true);
                openLatch.countDown();
                synchronized (messageLock) {
                    messageLock.notifyAll();
                }
            }
        };

        WebSocket webSocket = HttpSingleRequestExecutor.executeWebSocket(req, listener);
        activeWebSockets.add(webSocket);
        if (sessionRegistered.compareAndSet(false, true)) {
            realtimeMetrics.recordWebSocketSessionStart(webSocket, requestStartTime, apiId, apiName);
        }

        try {
            boolean opened = openLatch.await(Math.max(100, requestCfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
            if (!opened && !failed.get() && !interrupted.get()) {
                failed.set(true);
                errorRef.set("WebSocket connection timeout");
                closingSocket.set(true);
                webSocket.cancel();
            }

            if (!failed.get() && !interrupted.get()) {
                WebSocketScenarioStepCursor scenarioSteps = new WebSocketScenarioStepCursor(requestNode, runningSupplier);
                while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get()) {
                    DefaultMutableTreeNode childNode = scenarioSteps.next();
                    if (childNode == null) {
                        break;
                    }
                    Object childObj = childNode.getUserObject();
                    if (!(childObj instanceof JMeterTreeNode stepNode)) {
                        continue;
                    }
                    switch (stepNode.type) {
                        case WS_CONNECT -> {
                            // connect step is handled before scenario execution
                        }
                        case WS_SEND -> {
                            WebSocketPerformanceData stepCfg = stepNode.webSocketPerformanceData != null
                                    ? stepNode.webSocketPerformanceData
                                    : copyData(requestCfg);
                            lastStepCfgRef.set(stepCfg);
                            WebSocketPerformanceData.SendContentSource contentSource = stepCfg.sendContentSource != null
                                    ? stepCfg.sendContentSource
                                    : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
                            if (stepCfg.sendMode == WebSocketPerformanceData.SendMode.NONE
                                    || (contentSource == WebSocketPerformanceData.SendContentSource.REQUEST_BODY
                                    && CharSequenceUtil.isBlank(resolveSendPayloadTemplate(req, requestBodyTemplate, stepCfg)))) {
                                break;
                            }
                            int sendTimes = stepCfg.sendMode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT
                                    ? Math.max(1, stepCfg.sendCount)
                                    : 1;
                            int intervalMs = Math.max(0, stepCfg.sendIntervalMs);
                            for (int sendIndex = 0; sendIndex < sendTimes && runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get(); sendIndex++) {
                                ScriptExecutionResult sendScriptResult = executeSendPreScript(
                                        pipeline,
                                        stepCfg,
                                        sendIndex,
                                        sendTimes,
                                        stepNode.name
                                );
                                if (!sendScriptResult.isSuccess()) {
                                    failed.set(true);
                                    errorRef.set("WebSocket send pre-script failed: " + sendScriptResult.getErrorMessage());
                                    break;
                                }
                                String payload = resolveSendPayload(req, requestBodyTemplate, stepCfg);
                                boolean sent = webSocket.send(payload == null ? "" : payload);
                                if (sent) {
                                    sentMessageCount.incrementAndGet();
                                    realtimeMetrics.recordWebSocketSent(webSocket);
                                } else {
                                    failed.set(true);
                                    errorRef.set("WebSocket send failed");
                                    break;
                                }
                                if (sendIndex < sendTimes - 1 && intervalMs > 0) {
                                    TimeUnit.MILLISECONDS.sleep(intervalMs);
                                }
                            }
                        }
                        case WS_AWAIT -> {
                            WebSocketPerformanceData stepCfg = stepNode.webSocketPerformanceData != null
                                    ? stepNode.webSocketPerformanceData
                                    : copyData(requestCfg);
                            lastStepCfgRef.set(stepCfg);
                            long awaitStartTime = System.currentTimeMillis();
                            long firstMatchTime = -1;
                            int stepMatchedCount = 0;
                            BoundedTextAccumulator stepBody = new BoundedTextAccumulator(responseBodyPreviewLimitBytes);
                            boolean completed = false;
                            while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get() && !completed) {
                                synchronized (messageLock) {
                                    while (!receivedMessages.isEmpty()) {
                                        ReceivedWebSocketMessage message = receivedMessages.removeFirst();
                                        receivedMessagesRetainedBytes.addAndGet(-message.retainedUtf8Bytes());
                                        String payload = message.payload();
                                        boolean match = switch (stepCfg.completionMode) {
                                            case FIRST_MESSAGE -> true;
                                            default -> matchesMessage(stepCfg, payload);
                                        };
                                        if (!match) {
                                            continue;
                                        }
                                        if (firstMatchTime < 0) {
                                            firstMatchTime = message.receivedAtMs();
                                            firstMessageLatencyMs.compareAndSet(
                                                    -1,
                                                    Math.max(0, firstMatchTime - requestStartTime)
                                            );
                                        }
                                        stepMatchedCount++;
                                        matchedMessageCount.incrementAndGet();
                                        realtimeMetrics.recordWebSocketMatched(webSocket);
                                        appendMessage(stepBody, payload);
                                        if (stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE
                                                || stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MATCHED_MESSAGE) {
                                            completed = true;
                                            break;
                                        }
                                        if (stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                && stepMatchedCount >= Math.max(1, stepCfg.targetMessageCount)) {
                                            completed = true;
                                            break;
                                        }
                                    }
                                    if (completed) {
                                        break;
                                    }
                                    if (remoteClosed.get()) {
                                        failed.set(true);
                                        errorRef.set("WebSocket connection closed before await completed");
                                        break;
                                    }
                                    long now = System.currentTimeMillis();
                                    long deadline = switch (stepCfg.completionMode) {
                                        case FIRST_MESSAGE, MATCHED_MESSAGE ->
                                                awaitStartTime + Math.max(100, stepCfg.firstMessageTimeoutMs);
                                        case FIXED_DURATION -> awaitStartTime + Math.max(100, stepCfg.holdConnectionMs);
                                        case MESSAGE_COUNT -> (firstMatchTime < 0
                                                ? awaitStartTime + Math.max(100, stepCfg.firstMessageTimeoutMs)
                                                : firstMatchTime + Math.max(100, stepCfg.holdConnectionMs));
                                    };
                                    if (stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.FIXED_DURATION) {
                                        if (now >= deadline) {
                                            completed = true;
                                            break;
                                        }
                                    } else if (now >= deadline) {
                                        failed.set(true);
                                        errorRef.set(stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                ? "WebSocket target message count timeout"
                                                : "WebSocket await timeout");
                                        break;
                                    }
                                    long waitMs = Math.min(100, Math.max(1, deadline - now));
                                    messageLock.wait(waitMs);
                                }
                            }
                            HttpResponse stepResp = new HttpResponse();
                            stepResp.code = resp.code;
                            stepResp.protocol = resp.protocol;
                            stepResp.headers = resp.headers;
                            stepResp.body = stepBody.value();
                            stepResp.bodySize = stepBody.totalUtf8Bytes();
                            PerformanceAssertionRunner.runAssertionNodes(
                                    PerformanceAssertionRunner.collectDirectAssertionNodes(childNode),
                                    stepResp,
                                    stepTestResults,
                                    errorRef
                            );
                        }
                        case WS_CLOSE -> {
                            markSampleEnd(sampleEndTimeMs);
                            closingSocket.set(true);
                            try {
                                webSocket.close(1000, "WebSocket close step");
                            } catch (Exception ignored) {
                            }
                            scenarioSteps.stop();
                        }
                        case TIMER -> {
                            if (stepNode.timerData != null) {
                                TimeUnit.MILLISECONDS.sleep(stepNode.timerData.delayMs);
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            interrupted.set(true);
        } finally {
            markSampleEnd(sampleEndTimeMs);
            realtimeMetrics.recordWebSocketSessionEnd(webSocket);
            closingSocket.set(true);
            try {
                webSocket.close(1000, "Performance sample complete");
            } catch (Exception ignored) {
            }
            if (!failed.get() && !interrupted.get()) {
                try {
                    waitForSendQueueToDrain(webSocket);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    interrupted.set(true);
                }
            }
            webSocket.cancel();
            activeWebSockets.remove(webSocket);
        }

        long endTime = sampleEndTimeMs.get();
        resp.endTime = endTime;
        resp.costMs = endTime - requestStartTime;
        resp.body = responseBody.value();
        resp.bodySize = responseBody.totalUtf8Bytes();
        if (resp.headers == null) {
            resp.headers = new LinkedHashMap<>();
        }

        WebSocketPerformanceData headerCfg = lastStepCfgRef.get() != null ? lastStepCfgRef.get() : requestCfg;
        resp.addHeader("X-Easy-WS-Send-Mode", Collections.singletonList(headerCfg.sendMode.name()));
        WebSocketPerformanceData.SendContentSource contentSource = headerCfg.sendContentSource != null
                ? headerCfg.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        resp.addHeader("X-Easy-WS-Send-Content-Source", Collections.singletonList(contentSource.name()));
        resp.addHeader("X-Easy-WS-Send-Count-Configured", Collections.singletonList(String.valueOf(Math.max(1, headerCfg.sendCount))));
        resp.addHeader("X-Easy-WS-Send-Interval-Ms", Collections.singletonList(String.valueOf(Math.max(0, headerCfg.sendIntervalMs))));
        resp.addHeader("X-Easy-WS-Mode", Collections.singletonList(headerCfg.completionMode.name()));
        resp.addHeader("X-Easy-WS-Message-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(headerCfg.messageFilter, "")));
        resp.addHeader("X-Easy-WS-Received-Count", Collections.singletonList(String.valueOf(receivedMessageCount.get())));
        resp.addHeader("X-Easy-WS-Sent-Count", Collections.singletonList(String.valueOf(sentMessageCount.get())));
        resp.addHeader("X-Easy-WS-Message-Count", Collections.singletonList(String.valueOf(matchedMessageCount.get())));
        resp.addHeader("X-Easy-WS-First-Message-Latency-Ms", Collections.singletonList(firstMessageLatencyMs.get() >= 0 ? String.valueOf(firstMessageLatencyMs.get()) : ""));
        resp.addHeader("X-Easy-WS-Last-Message", Collections.singletonList(CharSequenceUtil.blankToDefault(lastMessageRef.get(), "")));
        if (CharSequenceUtil.isNotBlank(errorRef.get())) {
            resp.addHeader("X-Easy-WS-Error", Collections.singletonList(errorRef.get()));
        }

        return new Result(resp, errorRef.get(), failed.get(), interrupted.get(), stepTestResults);
    }

    private record ReceivedWebSocketMessage(String payload, long receivedAtMs, int retainedUtf8Bytes) {
    }

    private static void markSampleEnd(AtomicLong sampleEndTimeMs) {
        sampleEndTimeMs.compareAndSet(0, System.currentTimeMillis());
    }

    private boolean matchesMessage(WebSocketPerformanceData cfg, String payload) {
        if (!WebSocketPerformanceData.usesMessageFilter(cfg.completionMode)) {
            return true;
        }
        String filter = cfg.messageFilter;
        return CharSequenceUtil.isBlank(filter) || CharSequenceUtil.contains(payload, filter.trim());
    }

    private void waitForSendQueueToDrain(WebSocket webSocket) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
        while (webSocket.queueSize() > 0 && System.nanoTime() < deadline && runningSupplier.getAsBoolean()) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
    }

    private ScriptExecutionResult executeSendPreScript(ScriptExecutionPipeline pipeline,
                                                       WebSocketPerformanceData cfg,
                                                       int sendIndex,
                                                       int sendCount,
                                                       String stepName) {
        if (pipeline == null) {
            return ScriptExecutionResult.success();
        }
        String script = cfg != null ? cfg.sendPreScript : null;
        return pipeline.executeWebSocketSendScript(script, sendIndex, sendCount, stepName);
    }

    private String resolveSendPayload(PreparedRequest req,
                                      String requestBodyTemplate,
                                      WebSocketPerformanceData cfg) {
        return VariableResolver.resolve(resolveSendPayloadTemplate(req, requestBodyTemplate, cfg));
    }

    private String resolveSendPayloadTemplate(PreparedRequest req,
                                              String requestBodyTemplate,
                                              WebSocketPerformanceData cfg) {
        WebSocketPerformanceData.SendContentSource contentSource = cfg.sendContentSource != null
                ? cfg.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        if (contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT) {
            return cfg.customSendBody == null ? "" : cfg.customSendBody;
        }
        if (requestBodyTemplate != null) {
            return requestBodyTemplate;
        }
        return req != null ? req.body : "";
    }

    private void appendMessage(BoundedTextAccumulator buffer, String payload) {
        String value = payload == null ? "" : payload;
        buffer.append(value);
        buffer.append("\n\n");
    }

    static String buildResponseBody(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (String message : messages) {
            String value = message == null ? "" : message;
            buffer.append(value).append("\n\n");
        }
        return buffer.toString();
    }

    private static void addReceivedMessage(Deque<ReceivedWebSocketMessage> messages,
                                           String payload,
                                           long receivedAtMs,
                                           AtomicLong retainedBytes,
                                           int maxRetainedBytes) {
        int limit = Math.max(1, maxRetainedBytes);
        String retainedPayload = retainUtf8Prefix(payload, limit);
        int messageBytes = utf8Length(retainedPayload);
        while (!messages.isEmpty()
                && (messages.size() >= MAX_RETAINED_AWAIT_MESSAGES
                || retainedBytes.get() + messageBytes > limit)) {
            ReceivedWebSocketMessage removed = messages.removeFirst();
            retainedBytes.addAndGet(-removed.retainedUtf8Bytes());
        }
        messages.addLast(new ReceivedWebSocketMessage(retainedPayload, receivedAtMs, messageBytes));
        retainedBytes.addAndGet(messageBytes);
    }

    static String retainUtf8Prefix(String value, int maxUtf8Bytes) {
        if (value == null || value.isEmpty() || maxUtf8Bytes <= 0) {
            return "";
        }
        int bytes = 0;
        int index = 0;
        while (index < value.length()) {
            CharSpan span = charSpan(value, index, value.length());
            if (bytes + span.utf8Bytes > maxUtf8Bytes) {
                break;
            }
            bytes += span.utf8Bytes;
            index += span.charCount;
        }
        return value.substring(0, index);
    }

    private boolean hasEnabledAwaitStep(DefaultMutableTreeNode requestNode) {
        if (requestNode == null) {
            return false;
        }
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object childObj = childNode.getUserObject();
            if (childObj instanceof JMeterTreeNode stepNode && stepNode.enabled && stepNode.type == NodeType.WS_AWAIT) {
                return true;
            }
            if (childObj instanceof JMeterTreeNode stepNode && stepNode.enabled && stepNode.type == NodeType.LOOP
                    && hasEnabledAwaitStep(childNode)) {
                return true;
            }
        }
        return false;
    }

    private String headerPreview(String value) {
        if (value == null || value.length() <= 1024) {
            return value == null ? "" : value;
        }
        return value.substring(0, 1024) + "\n[truncated; total chars: " + value.length() + "]";
    }

    private String toHexPreview(ByteString bytes) {
        if (bytes == null || bytes.size() == 0) {
            return "";
        }
        int byteLimit = Math.min(bytes.size(), Math.max(1, responseBodyPreviewLimitBytes / 2));
        StringBuilder builder = new StringBuilder(byteLimit * 2 + 80);
        for (int i = 0; i < byteLimit; i++) {
            int value = bytes.getByte(i) & 0xff;
            if (value < 16) {
                builder.append('0');
            }
            builder.append(Integer.toHexString(value));
        }
        if (bytes.size() > byteLimit) {
            builder.append("\n\n[truncated binary message; total bytes: ")
                    .append(bytes.size())
                    .append(", retained bytes: ")
                    .append(byteLimit)
                    .append("]");
        }
        return builder.toString();
    }

    private static int utf8Length(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int bytes = 0;
        for (int i = 0; i < value.length(); ) {
            CharSpan span = charSpan(value, i, value.length());
            bytes += span.utf8Bytes;
            i += span.charCount;
        }
        return bytes;
    }

    private static CharSpan charSpan(CharSequence text, int index, int end) {
        char ch = text.charAt(index);
        if (ch <= 0x7F) {
            return new CharSpan(1, 1);
        }
        if (ch <= 0x7FF) {
            return new CharSpan(1, 2);
        }
        if (Character.isHighSurrogate(ch)
                && index + 1 < end
                && Character.isLowSurrogate(text.charAt(index + 1))) {
            return new CharSpan(2, 4);
        }
        return new CharSpan(1, 3);
    }

    private record CharSpan(int charCount, int utf8Bytes) {
    }

    private WebSocketPerformanceData copyData(WebSocketPerformanceData source) {
        WebSocketPerformanceData target = new WebSocketPerformanceData();
        if (source == null) {
            return target;
        }
        target.connectTimeoutMs = source.connectTimeoutMs;
        target.sendMode = source.sendMode;
        target.sendContentSource = source.sendContentSource != null
                ? source.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        target.customSendBody = source.customSendBody;
        target.sendPreScript = source.sendPreScript;
        target.sendCount = source.sendCount;
        target.sendIntervalMs = source.sendIntervalMs;
        target.completionMode = source.completionMode;
        target.firstMessageTimeoutMs = source.firstMessageTimeoutMs;
        target.holdConnectionMs = source.holdConnectionMs;
        target.targetMessageCount = source.targetMessageCount;
        target.messageFilter = source.messageFilter;
        return target;
    }
}
