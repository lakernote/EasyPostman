package com.laker.postman.plugin.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

final class CaptureSessionStore {
    private static final int MAX_FLOWS = 300;

    private final List<CaptureFlow> flows = new ArrayList<>();
    private final Map<String, CaptureFlow> flowById = new LinkedHashMap<>();
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
