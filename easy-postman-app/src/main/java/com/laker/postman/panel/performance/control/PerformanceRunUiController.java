package com.laker.postman.panel.performance.control;


import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class PerformanceRunUiController {

    public static final String ETA_STATUS_ICON = "icons/time.svg";
    public static final String REQUEST_STATUS_ICON = "icons/hash.svg";
    private static final String RUN_STATUS_ICON_PROPERTY = "easyPostman.runStatusIconPath";

    private final StartButton runButton;
    private final StopButton stopButton;
    private final RefreshButton refreshButton;
    private final List<JComponent> runLockedComponents;
    private final AtomicLong lastProgressSequence = new AtomicLong(0L);

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
        lastProgressSequence.set(0L);
        setProgressText(progressLabel, formatProgress(0, totalThreads));
    }

    public void updateProgressAsync(JLabel progressLabel, int activeThreads, int totalThreads) {
        SwingUtilities.invokeLater(() -> setProgressText(progressLabel, formatProgress(activeThreads, totalThreads)));
    }

    public void updateProgressAsync(JLabel progressLabel, int activeThreads, int totalThreads, long sequence) {
        if (sequence <= 0L) {
            updateProgressAsync(progressLabel, activeThreads, totalThreads);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (sequence < lastProgressSequence.get()) {
                return;
            }
            lastProgressSequence.set(sequence);
            setProgressText(progressLabel, formatProgress(activeThreads, totalThreads));
        });
    }

    public void updateRunStatus(JLabel statusLabel,
                                String statusText,
                                String iconPath) {
        if (statusLabel == null) {
            return;
        }
        if (statusText == null || statusText.isBlank()) {
            clearRunStatus(statusLabel);
            return;
        }
        statusLabel.setVisible(true);
        statusLabel.setText(statusText);
        statusLabel.setToolTipText(statusText);
        setStatusIcon(statusLabel, iconPath);
        statusLabel.revalidate();
        statusLabel.repaint();
    }

    public void updateRunStatusAsync(JLabel statusLabel,
                                     String statusText,
                                     String iconPath) {
        SwingUtilities.invokeLater(() -> updateRunStatus(statusLabel, statusText, iconPath));
    }

    public void clearRunStatus(JLabel statusLabel) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setVisible(false);
        statusLabel.setText("");
        statusLabel.setToolTipText(null);
        statusLabel.setIcon(null);
        statusLabel.putClientProperty(RUN_STATUS_ICON_PROPERTY, null);
        statusLabel.revalidate();
        statusLabel.repaint();
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

    private void setStatusIcon(JLabel label, String iconPath) {
        if (label == null || iconPath == null || iconPath.isBlank()) {
            return;
        }
        Object currentPath = label.getClientProperty(RUN_STATUS_ICON_PROPERTY);
        if (Objects.equals(currentPath, iconPath)) {
            return;
        }
        label.setIcon(new FlatSVGIcon(iconPath, 18, 18)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary())));
        label.putClientProperty(RUN_STATUS_ICON_PROPERTY, iconPath);
    }

    private void setRunLockedComponentsEnabled(boolean enabled) {
        for (JComponent component : runLockedComponents) {
            if (component != null) {
                component.setEnabled(enabled);
            }
        }
    }
}
