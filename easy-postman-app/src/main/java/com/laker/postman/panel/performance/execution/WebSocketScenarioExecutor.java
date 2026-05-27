package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformanceExtractorElement;
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
        return execute(req, requestSampler, requestCfg, requestBodyTemplate, pipeline,
                PerformanceResponseCapturePlan.resolve(true, requestSampler, false, true,
                        req == null ? "" : req.postscript),
                apiId, apiName);
    }

    public Result execute(PreparedRequest req,
                          PerformanceRequestSampler requestSampler,
                          WebSocketPerformanceData requestCfg,
                          String requestBodyTemplate,
                          ScriptExecutionPipeline pipeline,
                          PerformanceResponseCapturePlan capturePlan,
                          String apiId,
                          String apiName) {
        WebSocketPerformanceData baseRequestCfg = requestCfg == null ? new WebSocketPerformanceData() : requestCfg;
        long requestStartTime = System.currentTimeMillis();
        HttpResponse resp = new HttpResponse();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> errorRef = new AtomicReference<>("");
        AtomicReference<String> lastMessageRef = new AtomicReference<>("");
        AtomicReference<WebSocketPerformanceData> lastStepCfgRef = new AtomicReference<>(baseRequestCfg);
        PerformanceResponseCapturePlan effectiveCapturePlan = capturePlan == null
                ? PerformanceResponseCapturePlan.resolve(true, requestSampler, false, true,
                        req == null ? "" : req.postscript)
                : capturePlan;
        boolean retainReadPayloads = effectiveCapturePlan.retainWebSocketReadPayloads();
        boolean retainResponseBody = effectiveCapturePlan.retainStreamResponseBody();
        int retainedReadMessageLimit = retainReadPayloads
                ? WebSocketReceivedMessageBuffer.DEFAULT_MAX_RETAINED_READ_MESSAGES
                : WebSocketScenarioStepSupport.maxBufferedMessagesNeededForRead(requestSampler);
        boolean trackResponseBodySize = retainResponseBody || effectiveCapturePlan.trackStreamResponseBodySize();
        BoundedTextAccumulator responseBodySizeCounter = trackResponseBodySize
                ? new BoundedTextAccumulator(0)
                : null;
        AtomicReference<String> latestResponseBodyRef = new AtomicReference<>("");
        AtomicLong sampleEndTimeMs = new AtomicLong(0);
        AtomicLong firstMessageLatencyMs = new AtomicLong(-1);
        AtomicBoolean firstReceivedMessageRecorded = new AtomicBoolean(false);
        AtomicInteger receivedMessageCount = new AtomicInteger(0);
        AtomicInteger matchedMessageCount = new AtomicInteger(0);
        AtomicInteger sentMessageCount = new AtomicInteger(0);
        List<TestResult> stepTestResults = new ArrayList<>();
        WebSocketReceivedMessageBuffer receivedMessages = new WebSocketReceivedMessageBuffer(
                responseBodyPreviewLimitBytes,
                retainReadPayloads,
                retainedReadMessageLimit
        );
        boolean keepReceivedMessages = WebSocketScenarioStepSupport.hasEnabledReadStep(requestSampler);
        Object messageLock = new Object();
        class WebSocketScenarioSession {
            private final long startTimeMs = System.currentTimeMillis();
            private final CountDownLatch openLatch = new CountDownLatch(1);
            private final AtomicBoolean closingSocket = new AtomicBoolean(false);
            private final AtomicBoolean remoteClosed = new AtomicBoolean(false);
            private final AtomicBoolean registered = new AtomicBoolean(false);
            private WebSocket webSocket;
            private boolean ended;
        }

        class WebSocketSessionManager {
            private final List<WebSocketScenarioSession> sessions = new ArrayList<>();
            private WebSocketScenarioSession currentSession;

            WebSocketScenarioSession currentOpenSession() {
                if (currentSession == null || currentSession.webSocket == null || currentSession.remoteClosed.get()) {
                    return null;
                }
                return currentSession;
            }

            WebSocketScenarioSession open(WebSocketPerformanceData cfg) throws InterruptedException {
                WebSocketPerformanceData connectCfg = cfg == null ? baseRequestCfg : cfg;
                WebSocketScenarioSession session = new WebSocketScenarioSession();
                WebSocketListener listener = new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        resp.headers = new LinkedHashMap<>();
                        for (String name : response.headers().names()) {
                            resp.addHeader(name, response.headers(name));
                        }
                        resp.code = response.code();
                        resp.protocol = response.protocol().toString();
                        recordStart(session, webSocket);
                        session.openLatch.countDown();
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
                        if (retainResponseBody) {
                            latestResponseBodyRef.set(
                                    WebSocketReceivedMessageBuffer.retainUtf8Prefix(value, responseBodyPreviewLimitBytes)
                            );
                        }
                        if (responseBodySizeCounter != null) {
                            responseBodySizeCounter.append(value);
                        }
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
                        session.remoteClosed.set(true);
                        session.openLatch.countDown();
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
                        if (!session.closingSocket.get()) {
                            if (!runningSupplier.getAsBoolean() || cancelledChecker.test(throwable)) {
                                interrupted.set(true);
                            } else {
                                failed.set(true);
                                errorRef.set(CharSequenceUtil.blankToDefault(message, "WebSocket request failed"));
                            }
                        }
                        session.remoteClosed.set(true);
                        session.openLatch.countDown();
                        synchronized (messageLock) {
                            messageLock.notifyAll();
                        }
                    }
                };

                WebSocket webSocket = HttpSingleRequestExecutor.executeWebSocket(req, listener);
                session.webSocket = webSocket;
                sessions.add(session);
                currentSession = session;
                activeWebSockets.add(webSocket);
                recordStart(session, webSocket);

                boolean opened = session.openLatch.await(Math.max(100, connectCfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
                if (!opened && !failed.get() && !interrupted.get()) {
                    failed.set(true);
                    errorRef.set("WebSocket connection timeout");
                    close(session, "WebSocket connection timeout");
                }
                return session;
            }

            void closeCurrent(String reason) {
                WebSocketScenarioSession session = currentSession;
                if (session == null) {
                    return;
                }
                close(session, reason);
                if (currentSession == session) {
                    currentSession = null;
                }
            }

            void closeAll(String reason) {
                for (WebSocketScenarioSession session : sessions) {
                    close(session, reason);
                }
                currentSession = null;
            }

            private void recordStart(WebSocketScenarioSession session, WebSocket webSocket) {
                if (session.registered.compareAndSet(false, true)) {
                    realtimeMetrics.recordWebSocketSessionStart(webSocket, session.startTimeMs, apiId, apiName);
                }
            }

            private void close(WebSocketScenarioSession session, String reason) {
                if (session == null || session.ended) {
                    return;
                }
                session.closingSocket.set(true);
                WebSocket webSocket = session.webSocket;
                if (webSocket != null) {
                    try {
                        webSocket.close(1000, reason);
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
                    realtimeMetrics.recordWebSocketSessionEnd(webSocket);
                }
                session.ended = true;
            }
        }

        WebSocketSessionManager sessionManager = new WebSocketSessionManager();

        try {
            if (!failed.get() && !interrupted.get()) {
                WebSocketScenarioPlanStepCursor scenarioSteps = new WebSocketScenarioPlanStepCursor(requestSampler, runningSupplier);
                boolean implicitConnectAllowed = true;
                while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get()) {
                    PerformancePlanElement stepElement = scenarioSteps.next();
                    if (stepElement == null) {
                        break;
                    }
                    switch (stepElement.getType()) {
                        case WS_CONNECT -> {
                            WebSocketPerformanceData stepCfg = WebSocketScenarioStepSupport.webSocketData(stepElement, baseRequestCfg);
                            lastStepCfgRef.set(stepCfg);
                            sessionManager.closeCurrent("WebSocket reconnect step");
                            if (!failed.get() && !interrupted.get()) {
                                sessionManager.open(stepCfg);
                            }
                            implicitConnectAllowed = false;
                        }
                        case WS_SEND -> {
                            WebSocketPerformanceData stepCfg = WebSocketScenarioStepSupport.webSocketData(stepElement, baseRequestCfg);
                            lastStepCfgRef.set(stepCfg);
                            if (stepCfg.sendMode == WebSocketPerformanceData.SendMode.NONE
                                    || !WebSocketScenarioStepSupport.hasSendPayload(req, requestBodyTemplate, stepCfg)) {
                                break;
                            }
                            WebSocketScenarioSession session = sessionManager.currentOpenSession();
                            if (session == null) {
                                if (implicitConnectAllowed) {
                                    session = sessionManager.open(baseRequestCfg);
                                    implicitConnectAllowed = false;
                                } else {
                                    failed.set(true);
                                    errorRef.set("WebSocket connection is not open");
                                    break;
                                }
                            }
                            if (session == null || failed.get() || interrupted.get()) {
                                break;
                            }
                            WebSocket webSocket = session.webSocket;
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
                        case WS_READ -> {
                            WebSocketPerformanceData stepCfg = WebSocketScenarioStepSupport.webSocketData(stepElement, baseRequestCfg);
                            lastStepCfgRef.set(stepCfg);
                            WebSocketScenarioSession session = sessionManager.currentOpenSession();
                            if (session == null) {
                                if (implicitConnectAllowed) {
                                    session = sessionManager.open(baseRequestCfg);
                                    implicitConnectAllowed = false;
                                } else {
                                    failed.set(true);
                                    errorRef.set("WebSocket connection is not open");
                                    break;
                                }
                            }
                            if (session == null || failed.get() || interrupted.get()) {
                                break;
                            }
                            WebSocket webSocket = session.webSocket;
                            WebSocketPerformanceData.CompletionMode readMode = stepCfg.completionMode == null
                                    ? WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE
                                    : stepCfg.completionMode;
                            long readStartTime = System.currentTimeMillis();
                            long firstMatchTime = -1;
                            int stepMatchedCount = 0;
                            List<PerformanceAssertionElement> stepAssertions =
                                    stepElement instanceof PerformanceProtocolStageElement stage
                                            ? PerformanceAssertionRunner.collectDirectAssertionElements(stage.getElements())
                                            : List.of();
                            List<PerformanceExtractorElement> stepExtractors =
                                    stepElement instanceof PerformanceProtocolStageElement stage
                                            ? PerformanceExtractorRunner.collectDirectExtractorElements(stage.getElements())
                                            : List.of();
                            boolean stepRequiresResponseBody =
                                    PerformanceAssertionRunner.requiresResponseBodyElements(stepAssertions)
                                            || PerformanceExtractorRunner.requiresResponseBodyElements(stepExtractors);
                            String stepAssertionPayload = "";
                            boolean completed = false;
                            while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get() && !completed) {
                                synchronized (messageLock) {
                                    while (!receivedMessages.isEmpty()) {
                                        WebSocketReceivedMessageBuffer.Message message = receivedMessages.removeFirst();
                                        String payload = message.payload();
                                        boolean match = switch (readMode) {
                                            case SINGLE_MESSAGE -> true;
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
                                        if (stepRequiresResponseBody) {
                                            stepAssertionPayload = payload;
                                        }
                                        if (readMode == WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE
                                                || readMode == WebSocketPerformanceData.CompletionMode.UNTIL_MATCH) {
                                            completed = true;
                                            break;
                                        }
                                        if (readMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                && stepMatchedCount >= Math.max(1, stepCfg.targetMessageCount)) {
                                            completed = true;
                                            break;
                                        }
                                    }
                                    if (completed) {
                                        break;
                                    }
                                    if (session.remoteClosed.get()) {
                                        failed.set(true);
                                        errorRef.set("WebSocket connection closed before read completed");
                                        break;
                                    }
                                    long now = System.currentTimeMillis();
                                    long deadline = switch (readMode) {
                                        case SINGLE_MESSAGE, UNTIL_MATCH ->
                                                readStartTime + Math.max(100, stepCfg.firstMessageTimeoutMs);
                                        case FIXED_DURATION -> readStartTime + Math.max(100, stepCfg.holdConnectionMs);
                                        case MESSAGE_COUNT -> readStartTime + Math.max(100, stepCfg.firstMessageTimeoutMs);
                                    };
                                    if (readMode == WebSocketPerformanceData.CompletionMode.FIXED_DURATION) {
                                        if (now >= deadline) {
                                            completed = true;
                                            break;
                                        }
                                    } else if (now >= deadline) {
                                        failed.set(true);
                                        errorRef.set(readMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                ? "WebSocket target message count timeout"
                                                : "WebSocket read timeout");
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
                            if (stepRequiresResponseBody) {
                                BoundedTextAccumulator stepBody = new BoundedTextAccumulator(responseBodyPreviewLimitBytes);
                                stepBody.append(stepAssertionPayload);
                                stepResp.body = stepBody.value();
                                stepResp.bodySize = stepBody.totalUtf8Bytes();
                            } else {
                                stepResp.body = "";
                                stepResp.bodySize = 0;
                            }
                            PerformanceExtractorRunner.runExtractorElements(stepExtractors, stepResp);
                            PerformanceAssertionRunner.runAssertionElements(
                                    stepAssertions,
                                    stepResp,
                                    stepTestResults,
                                    errorRef
                            );
                        }
                        case WS_CLOSE -> {
                            if (scenarioSteps.peek() == null) {
                                markSampleEnd(sampleEndTimeMs);
                            }
                            sessionManager.closeCurrent("WebSocket close step");
                            implicitConnectAllowed = false;
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
            sessionManager.closeAll("Performance sample complete");
        }

        long endTime = sampleEndTimeMs.get();
        resp.endTime = endTime;
        resp.costMs = endTime - requestStartTime;
        if (retainResponseBody) {
            resp.body = latestResponseBodyRef.get();
        } else {
            resp.body = "";
        }
        resp.bodySize = responseBodySizeCounter == null ? 0 : responseBodySizeCounter.totalUtf8Bytes();
        if (resp.headers == null) {
            resp.headers = new LinkedHashMap<>();
        }

        WebSocketPerformanceData headerCfg = lastStepCfgRef.get() != null ? lastStepCfgRef.get() : baseRequestCfg;
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
