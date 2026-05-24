package com.laker.postman.panel.performance.control;

import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceRunUiControllerTest extends AbstractSwingUiTest {

    @Test
    public void markRunningAndIdleShouldToggleToolbarButtons() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StartButton runButton = new StartButton();
            StopButton stopButton = new StopButton();
            RefreshButton refreshButton = new RefreshButton();
            PerformanceRunUiController controller = new PerformanceRunUiController(
                    runButton,
                    stopButton,
                    refreshButton
            );

            controller.markRunning();
            assertFalse(runButton.isEnabled());
            assertTrue(stopButton.isEnabled());
            assertFalse(refreshButton.isEnabled());

            controller.markIdle();
            assertTrue(runButton.isEnabled());
            assertFalse(stopButton.isEnabled());
            assertTrue(refreshButton.isEnabled());
        });
    }

    @Test
    public void progressUpdatesShouldUseActiveAndTotalThreadCounts() throws Exception {
        AtomicReference<JLabel> progressLabelRef = new AtomicReference<>();
        AtomicReference<PerformanceRunUiController> controllerRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JLabel progressLabel = new JLabel();
            PerformanceRunUiController controller = new PerformanceRunUiController(
                    new StartButton(),
                    new StopButton(),
                    new RefreshButton()
            );
            controller.initializeProgress(progressLabel, 10);
            progressLabelRef.set(progressLabel);
            controllerRef.set(controller);
        });

        JLabel progressLabel = progressLabelRef.get();
        PerformanceRunUiController controller = controllerRef.get();
        assertEquals(progressLabel.getText(), "0/10");

        CountDownLatch updated = new CountDownLatch(1);
        controller.updateProgressAsync(progressLabel, 3, 10);
        SwingUtilities.invokeLater(updated::countDown);

        assertTrue(updated.await(1, TimeUnit.SECONDS));
        assertEquals(progressLabel.getText(), "3/10");
    }
}
