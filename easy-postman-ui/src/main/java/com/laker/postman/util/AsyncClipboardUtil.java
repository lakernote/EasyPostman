package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;
import lombok.experimental.UtilityClass;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Async wrappers for system clipboard access.
 * Clipboard providers can block while delivering data, so callers should not access them on the EDT.
 */
@Slf4j
@UtilityClass
public class AsyncClipboardUtil {
    public static final long DEFAULT_READ_TIMEOUT_MS = 800L;

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final ExecutorService CLIPBOARD_EXECUTOR = new ThreadPoolExecutor(
            0,
            1,
            30L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "easy-postman-clipboard-worker-" + THREAD_COUNTER.incrementAndGet()
                );
                thread.setDaemon(true);
                return thread;
            }
    );

    public static CompletableFuture<String> readStringAsync() {
        return readStringAsync(DEFAULT_READ_TIMEOUT_MS);
    }

    public static CompletableFuture<String> readStringAsync(long timeoutMs) {
        long effectiveTimeoutMs = Math.max(1L, timeoutMs);
        try {
            return CompletableFuture.supplyAsync(AsyncClipboardUtil::readStringSafely, CLIPBOARD_EXECUTOR)
                    .completeOnTimeout(null, effectiveTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            log.debug("Clipboard worker is busy; skipping clipboard read", ex);
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Void> setStringAsync(String text) {
        return writeStringAsync(text, "write");
    }

    public static CompletableFuture<Void> clearStringAsync() {
        return writeStringAsync("", "clear");
    }

    private static CompletableFuture<Void> writeStringAsync(String text, String operation) {
        try {
            return CompletableFuture.runAsync(() -> setStringSafely(text, operation), CLIPBOARD_EXECUTOR);
        } catch (RejectedExecutionException ex) {
            log.error("Clipboard worker is busy; skipping clipboard {}", operation, ex);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static String readStringSafely() {
        try {
            Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) content.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception ex) {
            log.debug("Read system clipboard text failed", ex);
        }
        return null;
    }

    private static void setStringSafely(String text, String operation) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text == null ? "" : text), null);
        } catch (Exception ex) {
            log.error("Failed to {} system clipboard text", operation, ex);
        }
    }

}
