package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceProtocolStageElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceTimerElement;
import com.laker.postman.panel.performance.timer.TimerData;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.util.ArrayList;
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
                          PerformanceRequestSampler requestSampler,
                          WebSocketPerformanceData requestCfg,
                          String requestBodyTemplate,
                          ScriptExecutionPipeline pipeline,
                          String apiId,
                          String apiName) {
        requestCfg = requestCfg == null ? new WebSocketPerformanceData() : requestCfg;
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
        WebSocketReceivedMessageBuffer receivedMessages = new WebSocketReceivedMessageBuffer(responseBodyPreviewLimitBytes);
        boolean keepReceivedMessages = WebSocketScenarioStepSupport.hasEnabledAwaitStep(requestSampler);
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
                        receivedMessages.add(value, receivedAtMs);
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
                WebSocketScenarioPlanStepCursor scenarioSteps = new WebSocketScenarioPlanStepCursor(requestSampler, runningSupplier);
                while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get()) {
                    PerformancePlanElement stepElement = scenarioSteps.next();
                    if (stepElement == null) {
                        break;
                    }
                    switch (stepElement.getType()) {
                        case WS_CONNECT -> {
                            // connect step is handled before scenario execution
                        }
                        case WS_SEND -> {
                            WebSocketPerformanceData stepCfg = WebSocketScenarioStepSupport.webSocketData(stepElement, requestCfg);
                            lastStepCfgRef.set(stepCfg);
                            if (stepCfg.sendMode == WebSocketPerformanceData.SendMode.NONE
                                    || !WebSocketScenarioStepSupport.hasSendPayload(req, requestBodyTemplate, stepCfg)) {
                                break;
                            }
                            int sendTimes = stepCfg.sendMode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT
                                    ? Math.max(1, stepCfg.sendCount)
                                    : 1;
                            int intervalMs = Math.max(0, stepCfg.sendIntervalMs);
                            for (int sendIndex = 0; sendIndex < sendTimes && runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get(); sendIndex++) {
                                var sendScriptResult = WebSocketScenarioStepSupport.executeSendPreScript(
                                        pipeline,
                                        stepCfg,
                                        sendIndex,
                                        sendTimes,
                                        stepElement.getName()
                                );
                                if (!sendScriptResult.isSuccess()) {
                                    failed.set(true);
                                    errorRef.set("WebSocket send pre-script failed: " + sendScriptResult.getErrorMessage());
                                    break;
                                }
                                String payload = WebSocketScenarioStepSupport.resolveSendPayload(req, requestBodyTemplate, stepCfg);
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
                            WebSocketPerformanceData stepCfg = WebSocketScenarioStepSupport.webSocketData(stepElement, requestCfg);
                            lastStepCfgRef.set(stepCfg);
                            long awaitStartTime = System.currentTimeMillis();
                            long firstMatchTime = -1;
                            int stepMatchedCount = 0;
                            BoundedTextAccumulator stepBody = new BoundedTextAccumulator(responseBodyPreviewLimitBytes);
                            boolean completed = false;
                            while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get() && !completed) {
                                synchronized (messageLock) {
                                    while (!receivedMessages.isEmpty()) {
                                        WebSocketReceivedMessageBuffer.Message message = receivedMessages.removeFirst();
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
                            PerformanceAssertionRunner.runAssertionElements(
                                    stepElement instanceof PerformanceProtocolStageElement stage
                                            ? PerformanceAssertionRunner.collectDirectAssertionElements(stage.getElements())
                                            : List.of(),
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
                            if (stepElement instanceof PerformanceTimerElement timerElement) {
                                TimerData timerData = timerElement.getTimerData();
                                if (timerData != null) {
                                    TimeUnit.MILLISECONDS.sleep(timerData.delayMs);
                                }
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
        WebSocketScenarioResponseBuilder.addSummaryHeaders(
                resp,
                headerCfg,
                receivedMessageCount.get(),
                sentMessageCount.get(),
                matchedMessageCount.get(),
                firstMessageLatencyMs.get(),
                lastMessageRef.get(),
                errorRef.get()
        );

        return new Result(resp, errorRef.get(), failed.get(), interrupted.get(), stepTestResults);
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

    private void appendMessage(BoundedTextAccumulator buffer, String payload) {
        String value = payload == null ? "" : payload;
        buffer.append(value);
        buffer.append("\n\n");
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

}
