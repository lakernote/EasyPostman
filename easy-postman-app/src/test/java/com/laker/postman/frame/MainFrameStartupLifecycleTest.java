package com.laker.postman.frame;

import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class MainFrameStartupLifecycleTest {

    @Test
    public void shouldAllowMainContentLoadRequestOnlyOnce() {
        MainFrameStartupLifecycle lifecycle = new MainFrameStartupLifecycle();

        assertTrue(lifecycle.markMainContentLoadRequested());
        assertFalse(lifecycle.markMainContentLoadRequested());
    }

    @Test
    public void shouldNotifyCallbacksRegisteredBeforeAndAfterMainContentLoaded() throws Exception {
        MainFrameStartupLifecycle lifecycle = new MainFrameStartupLifecycle();
        CountDownLatch callbacks = new CountDownLatch(2);
        AtomicBoolean callbackRanOnEdt = new AtomicBoolean(false);

        lifecycle.whenMainContentLoaded(() -> {
            callbackRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            callbacks.countDown();
        });
        lifecycle.markMainContentLoaded();
        lifecycle.whenMainContentLoaded(callbacks::countDown);

        assertTrue(callbacks.await(2, TimeUnit.SECONDS));
        assertTrue(callbackRanOnEdt.get());
    }

    @Test
    public void shouldNotifyFailureCallbacksRegisteredBeforeAndAfterFailure() throws Exception {
        MainFrameStartupLifecycle lifecycle = new MainFrameStartupLifecycle();
        RuntimeException failure = new RuntimeException("boom");
        CountDownLatch callbacks = new CountDownLatch(2);
        AtomicReference<Throwable> deliveredFailure = new AtomicReference<>();

        lifecycle.whenMainContentLoadFailed(throwable -> {
            deliveredFailure.set(throwable);
            callbacks.countDown();
        });
        lifecycle.markMainContentLoadFailed(failure);
        lifecycle.whenMainContentLoadFailed(throwable -> {
            deliveredFailure.set(throwable);
            callbacks.countDown();
        });

        assertTrue(callbacks.await(2, TimeUnit.SECONDS));
        assertSame(deliveredFailure.get(), failure);
    }

    @Test
    public void shouldNotifyStartupShellPaintedCallbacksRegisteredBeforeAndAfterPaint() throws Exception {
        MainFrameStartupLifecycle lifecycle = new MainFrameStartupLifecycle();
        CountDownLatch callbacks = new CountDownLatch(2);
        AtomicBoolean callbackRanOnEdt = new AtomicBoolean(false);

        lifecycle.whenStartupShellPainted(() -> {
            callbackRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            callbacks.countDown();
        });
        lifecycle.markStartupShellPainted();
        lifecycle.whenStartupShellPainted(callbacks::countDown);

        assertTrue(callbacks.await(2, TimeUnit.SECONDS));
        assertTrue(callbackRanOnEdt.get());
    }
}
