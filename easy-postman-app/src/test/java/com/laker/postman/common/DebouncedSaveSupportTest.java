package com.laker.postman.common;

import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DebouncedSaveSupportTest {

    @Test(description = "连续触发保存时应合并为一次执行")
    public void shouldCoalesceRepeatedSaveRequests() throws Exception {
        AtomicInteger saveCount = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        DebouncedSaveSupport support = new DebouncedSaveSupport(50, () -> {
            saveCount.incrementAndGet();
            latch.countDown();
        });

        SwingUtilities.invokeAndWait(() -> {
            support.requestSave();
            support.requestSave();
            support.requestSave();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertEquals(saveCount.get(), 1);
    }

}
