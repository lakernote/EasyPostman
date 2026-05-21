package com.laker.postman.panel.performance;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceStatisticsCoordinatorTest extends AbstractSwingUiTest {

    @Test
    public void refreshReportShouldReadSelectedTabOnEdt() throws Exception {
        ThreadCheckingTabbedPane tabbedPane = new ThreadCheckingTabbedPane();
        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                null,
                null,
                null,
                tabbedPane,
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                now -> null
        );

        coordinator.refreshReport();

        assertTrue(tabbedPane.awaitAccess());
        SwingUtilities.invokeAndWait(() -> {
        });
        assertFalse(tabbedPane.accessedOffEdt.get());
    }

    private static final class ThreadCheckingTabbedPane extends JTabbedPane {
        private final CountDownLatch accessed = new CountDownLatch(1);
        private final AtomicBoolean accessedOffEdt = new AtomicBoolean(false);

        @Override
        public int getSelectedIndex() {
            if (!SwingUtilities.isEventDispatchThread()) {
                accessedOffEdt.set(true);
            }
            accessed.countDown();
            return 0;
        }

        private boolean awaitAccess() throws InterruptedException {
            return accessed.await(1, TimeUnit.SECONDS);
        }
    }
}
