package com.laker.postman.panel.collections.tree;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

public class CollectionTreeToolbarTest extends AbstractSwingUiTest {

    @Test
    public void clipboardCurlDetectionShouldNotBlockWhenTransferDataIsSlow() throws Exception {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        CountDownLatch transferStarted = new CountDownLatch(1);
        CountDownLatch releaseTransfer = new CountDownLatch(1);
        CountDownLatch transferReturned = new CountDownLatch(1);
        CountDownLatch methodReturned = new CountDownLatch(1);

        String curlText = "curl https://example.com";
        clipboard.setContents(new SlowStringTransferable(
                curlText,
                transferStarted,
                releaseTransfer,
                transferReturned
        ), null);

        try {
            CollectionTreeToolbar toolbar = UiSingletonFactory.getInstance(CollectionTreeToolbar.class);
            CompletableFuture<String>[] futureHolder = new CompletableFuture[1];

            SwingUtilities.invokeLater(() -> {
                try {
                    futureHolder[0] = toolbar.readClipboardCurlTextAsync();
                } finally {
                    methodReturned.countDown();
                }
            });

            assertTrue(transferStarted.await(2, TimeUnit.SECONDS), "clipboard transfer should start");
            boolean returnedBeforeClipboardData = methodReturned.await(300, TimeUnit.MILLISECONDS);
            releaseTransfer.countDown();
            assertTrue(transferReturned.await(2, TimeUnit.SECONDS), "clipboard transfer should finish after release");
            assertTrue(methodReturned.await(2, TimeUnit.SECONDS), "clipboard detection method should eventually return");
            assertTrue(returnedBeforeClipboardData, "clipboard detection must not wait for clipboard transfer data on EDT");
            futureHolder[0].get(2, TimeUnit.SECONDS);
        } finally {
            releaseTransfer.countDown();
            clipboard.setContents(new StringSelection(""), null);
        }
    }

    private record SlowStringTransferable(
            String text,
            CountDownLatch transferStarted,
            CountDownLatch releaseTransfer,
            CountDownLatch transferReturned
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
            } finally {
                transferReturned.countDown();
            }
        }
    }
}
