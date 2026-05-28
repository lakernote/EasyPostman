package com.laker.postman.panel.performance.control;


import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import lombok.RequiredArgsConstructor;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

@RequiredArgsConstructor
public final class PerformanceRunUiController {

    private final StartButton runButton;
    private final StopButton stopButton;
    private final RefreshButton refreshButton;

    public void markRunning() {
        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        refreshButton.setEnabled(false);
    }

    public void markIdle() {
        runButton.setEnabled(true);
        stopButton.setEnabled(false);
        refreshButton.setEnabled(true);
    }

    public void initializeProgress(JLabel progressLabel, int totalThreads) {
        progressLabel.setText(formatProgress(0, totalThreads));
    }

    public void updateProgressAsync(JLabel progressLabel, int activeThreads, int totalThreads) {
        SwingUtilities.invokeLater(() -> progressLabel.setText(formatProgress(activeThreads, totalThreads)));
    }

    private String formatProgress(int activeThreads, int totalThreads) {
        return activeThreads + "/" + totalThreads;
    }
}
