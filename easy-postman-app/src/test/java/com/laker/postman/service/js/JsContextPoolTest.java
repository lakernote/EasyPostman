package com.laker.postman.service.js;

import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class JsContextPoolTest {

    @Test(description = "retiring a pool still releases borrowers already waiting for a context", timeOut = 3000)
    public void testRetiredPoolReleasesWaitingBorrowers() throws Exception {
        JsContextPool pool = new JsContextPool(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        JsContextPool.PooledContext firstBorrow = null;
        JsContextPool.PooledContext secondBorrow = null;
        try {
            firstBorrow = pool.borrowContext(1000);
            Future<JsContextPool.PooledContext> waitingBorrow = executor.submit(() -> pool.borrowContext(1000));

            long deadline = System.currentTimeMillis() + 1000;
            while (pool.getWaitingBorrowCountForTests() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertTrue(pool.getWaitingBorrowCountForTests() > 0, "second borrower should be waiting for the exhausted pool");

            pool.retire();
            pool.returnContext(firstBorrow);
            firstBorrow = null;

            secondBorrow = waitingBorrow.get(500, TimeUnit.MILLISECONDS);
            assertNotNull(secondBorrow.getContext());
        } finally {
            if (secondBorrow != null) {
                pool.returnContext(secondBorrow);
            }
            if (firstBorrow != null) {
                pool.returnContext(firstBorrow);
            }
            pool.shutdown();
            executor.shutdownNow();
        }
    }
}
