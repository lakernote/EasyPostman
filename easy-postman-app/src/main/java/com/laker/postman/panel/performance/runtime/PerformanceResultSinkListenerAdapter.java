package com.laker.postman.panel.performance.runtime;


import com.laker.postman.panel.performance.model.PerformanceResultListener;
import com.laker.postman.panel.performance.model.PerformanceSampleEvent;

import java.util.List;

public final class PerformanceResultSinkListenerAdapter implements PerformanceResultSink {
    private final List<PerformanceResultListener> listeners;

    public PerformanceResultSinkListenerAdapter(List<PerformanceResultListener> listeners) {
        this.listeners = List.copyOf(listeners == null ? List.of() : listeners);
    }

    @Override
    public void onSample(PerformanceSampleEvent event) {
        for (PerformanceResultListener listener : listeners) {
            listener.onSample(event);
        }
    }
}
