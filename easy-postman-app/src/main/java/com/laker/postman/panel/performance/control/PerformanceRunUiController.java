package com.laker.postman.panel.performance.control;


import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.List;

public final class PerformanceRunUiController {

    private final StartButton runButton;
    private final StopButton stopButton;
    private final RefreshButton refreshButton;
    private final List<JComponent> runLockedComponents;

    public PerformanceRunUiController(StartButton runButton,
                                      StopButton stopButton,
                                      RefreshButton refreshButton) {
        this(runButton, stopButton, refreshButton, List.of());
    }

    public PerformanceRunUiController(StartButton runButton,
                                      StopButton stopButton,
                                      RefreshButton refreshButton,
                                      List<JComponent> runLockedComponents) {
        this.runButton = runButton;
        this.stopButton = stopButton;
        this.refreshButton = refreshButton;
        this.runLockedComponents = runLockedComponents == null ? List.of() : List.copyOf(runLockedComponents);
    }

    public void markRunning() {
        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        refreshButton.setEnabled(false);
        setRunLockedComponentsEnabled(false);
    }

    public void markIdle() {
        runButton.setEnabled(true);
        stopButton.setEnabled(false);
        refreshButton.setEnabled(true);
        setRunLockedComponentsEnabled(true);
    }

    public void initializeProgress(JLabel progressLabel, int totalThreads) {
        setProgressText(progressLabel, formatProgress(0, totalThreads));
    }

    public void updateProgressAsync(JLabel progressLabel, int activeThreads, int totalThreads) {
        SwingUtilities.invokeLater(() -> setProgressText(progressLabel, formatProgress(activeThreads, totalThreads)));
    }

    private String formatProgress(int activeThreads, int totalThreads) {
        return activeThreads + "/" + totalThreads;
    }

    private void setProgressText(JLabel progressLabel, String text) {
        if (progressLabel == null) {
            return;
        }
        String safeText = text == null || text.isBlank() ? "0/0" : text;
        progressLabel.setText(safeText);
        progressLabel.setToolTipText(safeText);
    }

    private void setRunLockedComponentsEnabled(boolean enabled) {
        for (JComponent component : runLockedComponents) {
            if (component != null) {
                component.setEnabled(enabled);
            }
        }
    }
}
