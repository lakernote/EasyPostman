package com.laker.postman.panel.collections.editor;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

public class RequestEditorPlusTabControllerTest extends AbstractSwingUiTest {

    @Test
    public void plusTabClickShouldNotBlockWhenClipboardTransferDataIsSlow() throws Exception {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        CountDownLatch transferStarted = new CountDownLatch(1);
        CountDownLatch releaseTransfer = new CountDownLatch(1);
        CountDownLatch transferReturned = new CountDownLatch(1);
        CountDownLatch mouseHandlerReturned = new CountDownLatch(1);

        clipboard.setContents(new SlowStringTransferable(
                "plain clipboard text",
                transferStarted,
                releaseTransfer,
                transferReturned
        ), null);

        try {
            JTabbedPane tabbedPane = createPlusOnlyTabbedPane();
            RequestEditorPlusTabController controller = new RequestEditorPlusTabController(
                    tabbedPane,
                    tabbedPane,
                    "+",
                    "New Request",
                    (title, protocol) -> null
            );
            SwingUtilities.invokeAndWait(controller::install);

            SwingUtilities.invokeLater(() -> {
                try {
                    firePlusTabMousePressed(tabbedPane);
                } finally {
                    mouseHandlerReturned.countDown();
                }
            });

            assertTrue(transferStarted.await(2, TimeUnit.SECONDS), "clipboard transfer should start");
            boolean returnedBeforeClipboardData = mouseHandlerReturned.await(300, TimeUnit.MILLISECONDS);
            releaseTransfer.countDown();
            assertTrue(transferReturned.await(2, TimeUnit.SECONDS), "clipboard transfer should finish after release");
            assertTrue(mouseHandlerReturned.await(2, TimeUnit.SECONDS), "mouse handler should eventually return");
            assertTrue(returnedBeforeClipboardData, "plus-tab mouse handler must not wait for clipboard transfer data");
        } finally {
            releaseTransfer.countDown();
            clipboard.setContents(new StringSelection(""), null);
        }
    }

    private static JTabbedPane createPlusOnlyTabbedPane() throws Exception {
        JTabbedPane[] holder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("+", new JPanel());
            tabbedPane.setSize(120, 40);
            tabbedPane.doLayout();
            holder[0] = tabbedPane;
        });
        return holder[0];
    }

    private static void firePlusTabMousePressed(JTabbedPane tabbedPane) {
        Rectangle bounds = tabbedPane.getBoundsAt(0);
        MouseEvent event = new MouseEvent(
                tabbedPane,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                bounds.x + Math.max(1, bounds.width / 2),
                bounds.y + Math.max(1, bounds.height / 2),
                1,
                false,
                MouseEvent.BUTTON1
        );
        for (MouseListener listener : tabbedPane.getMouseListeners()) {
            listener.mousePressed(event);
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
