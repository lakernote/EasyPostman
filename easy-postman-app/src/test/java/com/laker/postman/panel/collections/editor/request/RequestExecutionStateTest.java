package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RequestExecutionStateTest {

    @Test
    public void shouldDisposeOpenConnectionsAndClearRunningState() {
        RequestExecutionState state = new RequestExecutionState();
        FakeRealtimeHandle eventSource = new FakeRealtimeHandle();
        FakeWebSocketConnection webSocket = new FakeWebSocketConnection();
        SwingWorker<Void, Void> worker = newNoopWorker();

        state.startWorker(worker);
        state.startSseConnection(eventSource);
        state.beginWebSocketConnection("ws-1");
        state.attachWebSocketConnection(webSocket);
        state.markAutoDetectedHttpSseOpen();

        state.disposeOpenConnections();

        assertTrue(state.isDisposed());
        assertTrue(state.isSseCancelled());
        assertTrue(eventSource.canceled);
        assertTrue(webSocket.closed);
        assertEquals(webSocket.closeCode, 1000);
        assertEquals(webSocket.closeReason, "Tab closed");
        assertTrue(worker.isCancelled());
        assertNull(state.currentEventSource());
        assertNull(state.currentWebSocket());
        assertNull(state.currentWebSocketConnectionId());
        assertNull(state.currentWorker());
        assertFalse(state.isAutoDetectedHttpSseOpen());
    }

    @Test
    public void shouldClearCurrentWorkerOnlyWhenIdentityMatches() {
        RequestExecutionState state = new RequestExecutionState();
        SwingWorker<Void, Void> oldWorker = newNoopWorker();
        SwingWorker<Void, Void> newWorker = newNoopWorker();

        state.startWorker(newWorker);
        state.clearCurrentWorkerIf(oldWorker);
        assertSame(state.currentWorker(), newWorker);

        state.clearCurrentWorkerIf(newWorker);
        assertNull(state.currentWorker());
    }

    @Test
    public void shouldTrackAutoDetectedHttpSseStateWithExplicitMethods() {
        RequestExecutionState state = new RequestExecutionState();

        state.markAutoDetectedHttpSseOpen();
        assertTrue(state.isAutoDetectedHttpSseOpen());

        state.clearAutoDetectedHttpSseOpen();
        assertFalse(state.isAutoDetectedHttpSseOpen());
    }

    @Test
    public void shouldTrackUserCancelledSseState() {
        RequestExecutionState state = new RequestExecutionState();

        state.markSseCancelled();
        assertTrue(state.isSseCancelled());

        state.resetSseCancelled();
        assertFalse(state.isSseCancelled());
    }

    private SwingWorker<Void, Void> newNoopWorker() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                return null;
            }
        };
    }

    private static class FakeRealtimeHandle implements RealtimeConnectionHandle {
        private boolean canceled;

        @Override
        public void cancel() {
            canceled = true;
        }
    }

    private static final class FakeWebSocketConnection extends FakeRealtimeHandle
            implements RealtimeWebSocketConnection {
        private boolean closed;
        private int closeCode;
        private String closeReason;

        @Override
        public boolean close(int code, String reason) {
            closed = true;
            closeCode = code;
            closeReason = reason;
            return true;
        }

        @Override
        public boolean send(String text) {
            return true;
        }

        @Override
        public long queueSize() {
            return 0;
        }
    }
}
