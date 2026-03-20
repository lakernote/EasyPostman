package com.laker.postman.panel.collections.right.request;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

final class WebSocketExecutionState {
    private final String connectionId;
    private final AtomicBoolean terminal = new AtomicBoolean(false);
    private final CountDownLatch completionSignal = new CountDownLatch(1);

    WebSocketExecutionState(String connectionId) {
        this.connectionId = connectionId;
    }

    boolean shouldHandleActiveCallback(String currentConnectionId, boolean disposed) {
        return !terminal.get()
                && !disposed
                && currentConnectionId != null
                && !currentConnectionId.isBlank()
                && connectionId.equals(currentConnectionId);
    }

    boolean markClosed() {
        return markTerminal();
    }

    boolean markFailed() {
        return markTerminal();
    }

    void awaitCompletion() throws InterruptedException {
        completionSignal.await();
    }

    boolean shouldSaveHistory(boolean disposed) {
        return terminal.get() && !disposed;
    }

    private boolean markTerminal() {
        if (!terminal.compareAndSet(false, true)) {
            return false;
        }
        completionSignal.countDown();
        return true;
    }
}
