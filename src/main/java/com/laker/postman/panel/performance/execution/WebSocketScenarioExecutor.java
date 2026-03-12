package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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

    public WebSocketScenarioExecutor(BooleanSupplier runningSupplier,
                                     Predicate<Throwable> cancelledChecker,
                                     Set<WebSocket> activeWebSockets) {
        this.runningSupplier = runningSupplier;
        this.cancelledChecker = cancelledChecker;
        this.activeWebSockets = activeWebSockets;
    }

    public Result execute(PreparedRequest req,
                          DefaultMutableTreeNode requestNode,
                          WebSocketPerformanceData requestCfg) {
        long requestStartTime = System.currentTimeMillis();
        HttpResponse resp = new HttpResponse();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean closingSocket = new AtomicBoolean(false);
        AtomicBoolean remoteClosed = new AtomicBoolean(false);
        AtomicReference<String> errorRef = new AtomicReference<>("");
        AtomicReference<String> completionReasonRef = new AtomicReference<>("pending");
        AtomicReference<String> lastMessageRef = new AtomicReference<>("");
        AtomicReference<WebSocketPerformanceData> lastStepCfgRef = new AtomicReference<>(requestCfg);
        StringBuffer matchedMessageBody = new StringBuffer();
        AtomicLong firstMessageLatencyMs = new AtomicLong(-1);
        AtomicInteger matchedMessageCount = new AtomicInteger(0);
        AtomicInteger sentMessageCount = new AtomicInteger(0);
        List<TestResult> stepTestResults = new ArrayList<>();
        List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());
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
                openLatch.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                appendMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
                appendMessage(bytes.hex());
            }

            private void appendMessage(String payload) {
                lastMessageRef.set(payload == null ? "" : payload);
                synchronized (messageLock) {
                    receivedMessages.add(payload == null ? "" : payload);
                    messageLock.notifyAll();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                remoteClosed.set(true);
                completionReasonRef.compareAndSet("pending", "closed");
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
                if (closingSocket.get()) {
                    completionReasonRef.compareAndSet("pending", "closed");
                } else if (!runningSupplier.getAsBoolean() || cancelledChecker.test(throwable)) {
                    interrupted.set(true);
                    completionReasonRef.set("interrupted");
                } else {
                    failed.set(true);
                    errorRef.set(CharSequenceUtil.blankToDefault(message, "WebSocket request failed"));
                    completionReasonRef.set("failure");
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

        try {
            boolean opened = openLatch.await(Math.max(100, requestCfg.connectTimeoutMs), TimeUnit.MILLISECONDS);
            if (!opened && !failed.get() && !interrupted.get()) {
                failed.set(true);
                errorRef.set("WebSocket connection timeout");
                completionReasonRef.set("connect_timeout");
                closingSocket.set(true);
                webSocket.cancel();
            }

            int messageCursor = 0;
            if (!failed.get() && !interrupted.get()) {
                for (int i = 0; i < requestNode.getChildCount() && runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get(); i++) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) requestNode.getChildAt(i);
                    Object childObj = childNode.getUserObject();
                    if (!(childObj instanceof JMeterTreeNode stepNode) || !stepNode.enabled) {
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
                            String payload = resolveSendPayload(req, stepCfg);
                            WebSocketPerformanceData.SendContentSource contentSource = stepCfg.sendContentSource != null
                                    ? stepCfg.sendContentSource
                                    : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
                            if (stepCfg.sendMode == WebSocketPerformanceData.SendMode.NONE
                                    || (contentSource == WebSocketPerformanceData.SendContentSource.REQUEST_BODY
                                    && CharSequenceUtil.isBlank(payload))) {
                                completionReasonRef.set("send_skipped");
                                break;
                            }
                            int sendTimes = stepCfg.sendMode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT
                                    ? Math.max(1, stepCfg.sendCount)
                                    : 1;
                            int intervalMs = Math.max(0, stepCfg.sendIntervalMs);
                            for (int sendIndex = 0; sendIndex < sendTimes && runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get(); sendIndex++) {
                                boolean sent = webSocket.send(payload == null ? "" : payload);
                                if (sent) {
                                    sentMessageCount.incrementAndGet();
                                    completionReasonRef.set("sent");
                                } else {
                                    failed.set(true);
                                    errorRef.set("WebSocket send failed");
                                    completionReasonRef.set("send_failed");
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
                            StringBuffer stepBody = new StringBuffer();
                            boolean completed = false;
                            while (runningSupplier.getAsBoolean() && !failed.get() && !interrupted.get() && !completed) {
                                synchronized (messageLock) {
                                    while (messageCursor < receivedMessages.size()) {
                                        String payload = receivedMessages.get(messageCursor++);
                                        boolean match = switch (stepCfg.completionMode) {
                                            case FIRST_MESSAGE -> true;
                                            default -> matchesMessage(stepCfg, payload);
                                        };
                                        if (!match) {
                                            continue;
                                        }
                                        if (firstMatchTime < 0) {
                                            firstMatchTime = System.currentTimeMillis();
                                            firstMessageLatencyMs.compareAndSet(-1, firstMatchTime - requestStartTime);
                                        }
                                        stepMatchedCount++;
                                        matchedMessageCount.incrementAndGet();
                                        appendMessage(stepBody, payload);
                                        appendMessage(matchedMessageBody, payload);
                                        if (stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE
                                                || stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MATCHED_MESSAGE) {
                                            completionReasonRef.set(stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE
                                                    ? "first_message" : "matched_message");
                                            completed = true;
                                            break;
                                        }
                                        if (stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                && stepMatchedCount >= Math.max(1, stepCfg.targetMessageCount)) {
                                            completionReasonRef.set("message_target");
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
                                        completionReasonRef.set("closed_early");
                                        break;
                                    }
                                    long now = System.currentTimeMillis();
                                    long deadline = switch (stepCfg.completionMode) {
                                        case FIRST_MESSAGE, MATCHED_MESSAGE -> awaitStartTime + Math.max(100, stepCfg.firstMessageTimeoutMs);
                                        case FIXED_DURATION -> awaitStartTime + Math.max(100, stepCfg.holdConnectionMs);
                                        case MESSAGE_COUNT -> (firstMatchTime < 0
                                                ? awaitStartTime + Math.max(100, stepCfg.firstMessageTimeoutMs)
                                                : firstMatchTime + Math.max(100, stepCfg.holdConnectionMs));
                                    };
                                    if (stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.FIXED_DURATION) {
                                        if (now >= deadline) {
                                            completionReasonRef.set("hold_complete");
                                            completed = true;
                                            break;
                                        }
                                    } else if (now >= deadline) {
                                        failed.set(true);
                                        errorRef.set(stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                ? "WebSocket target message count timeout"
                                                : "WebSocket await timeout");
                                        completionReasonRef.set(stepCfg.completionMode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                                                ? "message_target_timeout" : "await_timeout");
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
                            stepResp.body = stepBody.toString();
                            stepResp.bodySize = stepResp.body.getBytes(StandardCharsets.UTF_8).length;
                            PerformanceAssertionRunner.runAssertionNodes(
                                    PerformanceAssertionRunner.collectDirectAssertionNodes(childNode),
                                    stepResp,
                                    stepTestResults,
                                    errorRef
                            );
                        }
                        case WS_CLOSE -> {
                            completionReasonRef.set("closed_by_step");
                            closingSocket.set(true);
                            try {
                                webSocket.close(1000, "WebSocket close step");
                            } catch (Exception ignored) {
                            }
                            i = requestNode.getChildCount();
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
            completionReasonRef.set("interrupted");
        } finally {
            closingSocket.set(true);
            try {
                webSocket.close(1000, "Performance sample complete");
            } catch (Exception ignored) {
            }
            webSocket.cancel();
            activeWebSockets.remove(webSocket);
        }

        long endTime = System.currentTimeMillis();
        resp.endTime = endTime;
        resp.costMs = endTime - requestStartTime;
        resp.body = matchedMessageBody.toString();
        resp.bodySize = resp.body.getBytes(StandardCharsets.UTF_8).length;
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
        resp.addHeader("X-Easy-WS-Sent-Count", Collections.singletonList(String.valueOf(sentMessageCount.get())));
        resp.addHeader("X-Easy-WS-Message-Count", Collections.singletonList(String.valueOf(matchedMessageCount.get())));
        resp.addHeader("X-Easy-WS-First-Message-Latency-Ms", Collections.singletonList(firstMessageLatencyMs.get() >= 0 ? String.valueOf(firstMessageLatencyMs.get()) : ""));
        resp.addHeader("X-Easy-WS-Completion-Reason", Collections.singletonList(CharSequenceUtil.blankToDefault(completionReasonRef.get(), "")));
        resp.addHeader("X-Easy-WS-Last-Message", Collections.singletonList(CharSequenceUtil.blankToDefault(lastMessageRef.get(), "")));
        if (CharSequenceUtil.isNotBlank(errorRef.get())) {
            resp.addHeader("X-Easy-WS-Error", Collections.singletonList(errorRef.get()));
        }

        return new Result(resp, errorRef.get(), failed.get(), interrupted.get(), stepTestResults);
    }

    private boolean matchesMessage(WebSocketPerformanceData cfg, String payload) {
        String filter = cfg.messageFilter;
        return CharSequenceUtil.isBlank(filter) || CharSequenceUtil.contains(payload, filter.trim());
    }

    private String resolveSendPayload(PreparedRequest req, WebSocketPerformanceData cfg) {
        WebSocketPerformanceData.SendContentSource contentSource = cfg.sendContentSource != null
                ? cfg.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        if (contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT) {
            return cfg.customSendBody == null ? "" : cfg.customSendBody;
        }
        return req.body;
    }

    private void appendMessage(StringBuffer buffer, String payload) {
        String value = payload == null ? "" : payload;
        buffer.append(value).append("\n\n");
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
