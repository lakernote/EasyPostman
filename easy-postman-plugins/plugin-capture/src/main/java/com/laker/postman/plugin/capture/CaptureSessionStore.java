package com.laker.postman.plugin.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

final class CaptureSessionStore {
    static final int DEFAULT_MAX_FLOWS = 300;
    static final int MIN_MAX_FLOWS = 50;
    static final int HARD_MAX_FLOWS = 1_000;
    private static final int TLS_ISSUE_STATUS_CODE = 495;
    private static final long TLS_ISSUE_SUPPRESS_MS = 30_000L;

    private final List<CaptureFlow> flows = new ArrayList<>();
    private final Map<String, CaptureFlow> flowById = new LinkedHashMap<>();
    private final Map<String, Long> tlsIssueRecordedAt = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int maxFlows;

    CaptureSessionStore() {
        this(DEFAULT_MAX_FLOWS);
    }

    CaptureSessionStore(int maxFlows) {
        this.maxFlows = normalizeMaxFlows(maxFlows);
    }

    CaptureFlow createFlow(String method,
                           String url,
                           String host,
                           String path,
                           Map<String, String> requestHeaders,
                           byte[] requestBody) {
        return createFlow(method, url, host, path, requestHeaders, requestBody, "", CaptureSourceInfo.unknown(), List.of());
    }

    CaptureFlow createFlow(String method,
                           String url,
                           String host,
                           String path,
                           Map<String, String> requestHeaders,
                           byte[] requestBody,
                           String connectionId,
                           CaptureSourceInfo sourceInfo,
                           List<CaptureDiagnosticEvent> diagnosticEvents) {
        CaptureFlow flow = new CaptureFlow(
                method,
                url,
                host,
                path,
                requestHeaders,
                requestBody,
                connectionId,
                sourceInfo,
                diagnosticEvents
        );
        synchronized (this) {
            flows.add(0, flow);
            flowById.put(flow.id(), flow);
            trimToMaxFlows();
        }
        fireChanged();
        return flow;
    }

    synchronized int maxFlows() {
        return maxFlows;
    }

    void setMaxFlows(int maxFlows) {
        boolean changed;
        synchronized (this) {
            int normalized = normalizeMaxFlows(maxFlows);
            changed = this.maxFlows != normalized;
            this.maxFlows = normalized;
            trimToMaxFlows();
        }
        if (changed) {
            fireChanged();
        }
    }

    static int normalizeMaxFlows(int value) {
        if (value < MIN_MAX_FLOWS) {
            return DEFAULT_MAX_FLOWS;
        }
        return Math.min(value, HARD_MAX_FLOWS);
    }

    private void trimToMaxFlows() {
        while (flows.size() > maxFlows) {
            CaptureFlow removed = flows.remove(flows.size() - 1);
            flowById.remove(removed.id());
        }
    }

    boolean recordTlsIssue(String host, int port, String message) {
        return recordTlsIssue(host, port, message, "", CaptureSourceInfo.unknown(), List.of());
    }

    boolean recordTlsIssue(String host,
                           int port,
                           String message,
                           String connectionId,
                           CaptureSourceInfo sourceInfo,
                           List<CaptureDiagnosticEvent> diagnosticEvents) {
        String normalizedHost = host == null ? "" : host.trim();
        if (normalizedHost.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        String key = normalizedHost.toLowerCase(Locale.ROOT) + ":" + port;
        synchronized (this) {
            Long lastRecordedAt = tlsIssueRecordedAt.get(key);
            if (lastRecordedAt != null && now - lastRecordedAt < TLS_ISSUE_SUPPRESS_MS) {
                return false;
            }
            tlsIssueRecordedAt.put(key, now);
        }

        String url = "https://" + normalizedHost + (port == 443 ? "" : ":" + port) + "/";
        CaptureFlow flow = createFlow(
                "TLS",
                url,
                normalizedHost,
                "/",
                Map.of(),
                new byte[0],
                connectionId,
                sourceInfo,
                diagnosticEvents
        );
        fail(flow.id(), TLS_ISSUE_STATUS_CODE, message);
        return true;
    }

    void complete(String flowId, int statusCode, String statusText, Map<String, String> responseHeaders, byte[] responseBody) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.complete(statusCode, statusText, responseHeaders, responseBody);
            fireChanged();
        }
    }

    void recordResponseStart(String flowId, int statusCode, String statusText, Map<String, String> responseHeaders) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.recordResponseStart(statusCode, statusText, responseHeaders);
            fireChanged();
        }
    }

    void appendRequestBody(String flowId, byte[] bytes) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.appendRequestBody(bytes);
            fireChanged();
        }
    }

    void appendRequestStreamEvent(String flowId, String text) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.appendRequestStreamEvent(text);
            fireChanged();
        }
    }

    void appendResponseBody(String flowId, byte[] bytes) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.appendResponseBody(bytes);
            fireChanged();
        }
    }

    void appendResponseStreamEvent(String flowId, String text) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.appendResponseStreamEvent(text);
            fireChanged();
        }
    }

    void appendDiagnosticEvent(String flowId, CaptureDiagnosticEvent event) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.appendDiagnosticEvent(event);
            fireChanged();
        }
    }

    void appendDiagnosticEventForConnection(String connectionId, CaptureDiagnosticEvent event) {
        if (connectionId == null || connectionId.isBlank() || event == null) {
            return;
        }
        boolean changed = false;
        synchronized (this) {
            for (CaptureFlow flow : flows) {
                if (connectionId.equals(flow.connectionId())) {
                    flow.appendDiagnosticEvent(event);
                    changed = true;
                }
            }
        }
        if (changed) {
            fireChanged();
        }
    }

    void updateSourceForConnection(String connectionId, CaptureSourceInfo sourceInfo) {
        if (connectionId == null || connectionId.isBlank() || sourceInfo == null) {
            return;
        }
        boolean changed = false;
        synchronized (this) {
            for (CaptureFlow flow : flows) {
                if (connectionId.equals(flow.connectionId())) {
                    flow.updateSourceInfo(sourceInfo);
                    changed = true;
                }
            }
        }
        if (changed) {
            fireChanged();
        }
    }

    void complete(String flowId) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.complete();
            fireChanged();
        }
    }

    void fail(String flowId, int statusCode, String errorMessage) {
        CaptureFlow flow;
        synchronized (this) {
            flow = flowById.get(flowId);
        }
        if (flow != null) {
            flow.fail(statusCode, errorMessage);
            fireChanged();
        }
    }

    synchronized List<CaptureFlow> snapshot() {
        return new ArrayList<>(flows);
    }

    synchronized CaptureFlow find(String flowId) {
        return flowById.get(flowId);
    }

    synchronized void clear() {
        flows.clear();
        flowById.clear();
        tlsIssueRecordedAt.clear();
        fireChanged();
    }

    void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
