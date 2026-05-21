package com.laker.postman.panel.performance;

import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceExecutionEngineTest {

    @Test(timeOut = 3000)
    public void joinThreadGroupThreadsShouldInterruptChildrenAndWaitWhenInterrupted() throws Exception {
        CountDownLatch childStarted = new CountDownLatch(1);
        AtomicBoolean childInterrupted = new AtomicBoolean(false);
        AtomicBoolean cancellationCalled = new AtomicBoolean(false);

        Thread child = new Thread(() -> {
            childStarted.countDown();
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                childInterrupted.set(true);
                Thread.currentThread().interrupt();
            }
        });
        child.start();
        assertTrue(childStarted.await(500, TimeUnit.MILLISECONDS));

        Thread joiner = new Thread(() ->
                PerformanceExecutionEngine.joinThreadGroupThreads(List.of(child), () -> cancellationCalled.set(true))
        );
        joiner.start();

        Thread.sleep(100);
        joiner.interrupt();
        joiner.join(1000);

        assertFalse(joiner.isAlive());
        assertFalse(child.isAlive());
        assertTrue(childInterrupted.get());
        assertTrue(cancellationCalled.get());
    }
}
