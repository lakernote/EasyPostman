package com.laker.postman.util;

import org.testng.SkipException;
import org.testng.annotations.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class AsyncClipboardUtilTest {

    @Test
    public void readStringAsyncShouldTimeoutWhenClipboardTransferDataIsSlow() throws Exception {
        skipWhenClipboardUnavailable();

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        CountDownLatch transferStarted = new CountDownLatch(1);
        CountDownLatch releaseTransfer = new CountDownLatch(1);

        clipboard.setContents(new SlowStringTransferable(
                "curl https://example.com",
                transferStarted,
                releaseTransfer
        ), null);

        try {
            String result = AsyncClipboardUtil.readStringAsync(100).get(1, TimeUnit.SECONDS);

            assertTrue(transferStarted.await(1, TimeUnit.SECONDS), "clipboard transfer should start");
            assertNull(result);
        } finally {
            releaseTransfer.countDown();
            clipboard.setContents(new StringSelection(""), null);
        }
    }

    private static void skipWhenClipboardUnavailable() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new SkipException("Skipping clipboard test in headless environment");
        }
    }

    private record SlowStringTransferable(
            String text,
            CountDownLatch transferStarted,
            CountDownLatch releaseTransfer
    ) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            transferStarted.countDown();
            try {
                releaseTransfer.await(2, TimeUnit.SECONDS);
                return text;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for clipboard transfer", ex);
            }
        }
    }
}
