package com.laker.postman.performance.core.runtime;

public final class PerformanceRunHandle {
    private final Thread thread;
    private final Runnable stopAction;

    public PerformanceRunHandle(Thread thread, Runnable stopAction) {
        this.thread = thread;
        this.stopAction = stopAction == null ? () -> {
        } : stopAction;
    }

    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    public void join(long timeoutMs) throws InterruptedException {
        if (thread != null) {
            thread.join(timeoutMs);
        }
    }

    public void stop() {
        stopAction.run();
    }

    public Thread threadOrNull() {
        return thread;
    }
}
