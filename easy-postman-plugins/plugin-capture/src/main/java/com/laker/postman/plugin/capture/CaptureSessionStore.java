package com.laker.postman.plugin.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

final class CaptureSessionStore {
    private static final int MAX_FLOWS = 300;
    private static final int TLS_ISSUE_STATUS_CODE = 495;
    private static final long TLS_ISSUE_SUPPRESS_MS = 30_000L;

    private final List<CaptureFlow> flows = new ArrayList<>();
    private final Map<String, CaptureFlow> flowById = new LinkedHashMap<>();
    private final Map<String, Long> tlsIssueRecordedAt = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    CaptureFlow createFlow(String method,
                           String url,
                           String host,
                           String path,
                           Map<String, String> requestHeaders,
                           byte[] requestBody) {
        CaptureFlow flow = new CaptureFlow(method, url, host, path, requestHeaders, requestBody);
        synchronized (this) {
            flows.add(0, flow);
            flowById.put(flow.id(), flow);
            while (flows.size() > MAX_FLOWS) {
                CaptureFlow removed = flows.remove(flows.size() - 1);
                flowById.remove(removed.id());
            }
        }
        fireChanged();
        return flow;
    }

    void recordTlsIssue(String host, int port, String message) {
        String normalizedHost = host == null ? "" : host.trim();
        if (normalizedHost.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        String key = normalizedHost.toLowerCase(Locale.ROOT) + ":" + port;
        synchronized (this) {
            Long lastRecordedAt = tlsIssueRecordedAt.get(key);
            if (lastRecordedAt != null && now - lastRecordedAt < TLS_ISSUE_SUPPRESS_MS) {
                return;
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
                new byte[0]
        );
        fail(flow.id(), TLS_ISSUE_STATUS_CODE, message);
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
