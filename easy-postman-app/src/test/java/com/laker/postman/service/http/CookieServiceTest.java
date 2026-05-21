package com.laker.postman.service.http;

import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CookieServiceTest {

    @Test
    public void shouldCoalesceBackgroundNotificationsAndRunListenersOnEdt() throws Exception {
        CountDownLatch edtBlocked = new CountDownLatch(1);
        CountDownLatch releaseEdt = new CountDownLatch(1);
        CountDownLatch notified = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger();
        AtomicBoolean invokedOffEdt = new AtomicBoolean(false);

        Runnable listener = () -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                invokedOffEdt.set(true);
            }
            invocations.incrementAndGet();
            notified.countDown();
        };

        CookieService.registerCookieChangeListener(listener);
        try {
            SwingUtilities.invokeLater(() -> {
                edtBlocked.countDown();
                try {
                    releaseEdt.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(edtBlocked.await(1, TimeUnit.SECONDS));

            for (int i = 0; i < 100; i++) {
                CookieService.notifyCookieChanged();
            }

            releaseEdt.countDown();
            assertTrue(notified.await(1, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
            });

            assertFalse(invokedOffEdt.get());
            assertEquals(invocations.get(), 1);
        } finally {
            releaseEdt.countDown();
            CookieService.unregisterCookieChangeListener(listener);
        }
    }
}
