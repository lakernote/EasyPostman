package com.laker.postman.panel.performance.control;

import com.laker.postman.performance.core.runtime.PerformanceRunError;
import com.laker.postman.performance.core.runtime.PerformanceRunListener;
import com.laker.postman.performance.core.runtime.PerformanceRunProgress;


import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;

@RequiredArgsConstructor
public final class PerformanceRunUiEventBridge implements PerformanceRunListener {
    private final Component parentComponent;
    private final PerformanceRunUiController runUiController;
    private final JLabel progressLabel;

    @Override
    public void onProgress(PerformanceRunProgress progress) {
        if (progress == null || runUiController == null || progressLabel == null) {
            return;
        }
        runUiController.updateProgressAsync(
                progressLabel,
                progress.getActiveThreads(),
                progress.getTotalThreads()
        );
    }

    @Override
    public void onError(PerformanceRunError error) {
        if (error == null) {
            return;
        }
        String detail = resolveDetail(error);
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                parentComponent,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_EXECUTION_INTERRUPTED, detail),
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private static String resolveDetail(PerformanceRunError error) {
        if (error.getMessage() != null && !error.getMessage().isBlank()) {
            return error.getMessage();
        }
        Throwable cause = error.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return error.getClass().getSimpleName();
    }
}
