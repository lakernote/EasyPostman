package com.laker.postman.service.js;

import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
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

    @Test(description = "returned contexts should not retain request/response bindings from previous scripts")
    public void shouldCleanupInjectedBindingsBeforeContextReuse() throws Exception {
        JsContextPool pool = new JsContextPool(1);
        JsContextPool.PooledContext borrowed = null;
        try {
            borrowed = pool.borrowContext(1000);
            var context = borrowed.getContext();
            context.eval("js", """
                    globalThis.pm = {};
                    globalThis.postman = {};
                    globalThis.request = {};
                    globalThis.env = {};
                    globalThis.environment = {};
                    globalThis.globals = {};
                    globalThis.response = { body: 'large-body' };
                    globalThis.responseBody = 'large-body';
                    globalThis.responseHeaders = {};
                    globalThis.statusCode = 200;
                    globalThis.tests = {};
                    globalThis.iterationData = {};
                    """);
            pool.returnContext(borrowed);
            borrowed = null;

            borrowed = pool.borrowContext(1000);
            String retainedGlobals = borrowed.getContext().eval("js", """
                    [
                      'pm', 'postman', 'request', 'env', 'environment', 'globals',
                      'response', 'responseBody', 'responseHeaders', 'statusCode',
                      'tests', 'iterationData'
                    ].filter(name => typeof globalThis[name] !== 'undefined').join(',')
                    """).asString();

            assertEquals(retainedGlobals, "");
        } finally {
            if (borrowed != null) {
                pool.returnContext(borrowed);
            }
            pool.shutdown();
        }
    }
}
