package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.runtime.PerformanceRunHandle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PerformanceRunExecutionControl {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<PerformanceRunHandle> handle = new AtomicReference<>();

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public void bind(PerformanceRunHandle runHandle) {
        handle.set(runHandle);
    }

    public void stop() {
        running.set(false);
        PerformanceRunHandle runHandle = handle.get();
        if (runHandle != null) {
            runHandle.stop();
        }
    }
}
