package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.text.CharSequenceUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.panel.collections.right.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

import javax.swing.*;
import java.awt.event.ActionListener;

final class RequestPreparationFeedbackHelper {

    void clearUrlValidationFeedback(JTextField urlField) {
        urlField.putClientProperty(FlatClientProperties.OUTLINE, null);
        urlField.setToolTipText(null);
    }

    boolean handlePreparationFailure(RequestPreparationResult result,
                                     JTextField urlField,
                                     RequestLinePanel requestLinePanel,
                                     ActionListener sendAction,
                                     ResponsePanel responsePanel) {
        if (result == null) {
            resetUiAfterPreparationFailure(requestLinePanel, sendAction, responsePanel);
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE));
            return true;
        }
        if (result.hasValidationFailure()) {
            resetUiAfterPreparationFailure(requestLinePanel, sendAction, responsePanel);
            showValidationFailure(urlField, result.getValidationResult());
            return true;
        }
        if (result.hasError()) {
            resetUiAfterPreparationFailure(requestLinePanel, sendAction, responsePanel);
            NotificationUtil.showError(result.getErrorMessage());
            return true;
        }
        return false;
    }

    void showPreparationWarningIfNeeded(RequestPreparationResult result) {
        if (!result.hasWarning()) {
            return;
        }
        NotificationUtil.showWarning(result.getValidationResult().getMessage());
        ConsolePanel.appendLog("[Warning] " + result.getValidationResult().getMessage(), ConsolePanel.LogType.WARN);
    }

    private void resetUiAfterPreparationFailure(RequestLinePanel requestLinePanel,
                                                ActionListener sendAction,
                                                ResponsePanel responsePanel) {
        requestLinePanel.setSendButtonToSend(sendAction);
        responsePanel.hideLoadingOverlay();
    }

    private void showValidationFailure(JTextField urlField, HttpUtil.ValidationResult validationResult) {
        String message = validationResult.getMessage();
        if (CharSequenceUtil.isNotBlank(message)) {
            if (validationResult.isWarning()) {
                NotificationUtil.showWarning(message);
                ConsolePanel.appendLog("[Warning] " + message, ConsolePanel.LogType.WARN);
            } else {
                NotificationUtil.showError(message);
                ConsolePanel.appendLog("[Error] " + message, ConsolePanel.LogType.ERROR);
            }
        }
        if (validationResult.shouldFocusUrlField()) {
            showUrlValidationFeedback(urlField, message, validationResult.isWarning());
        }
    }

    private void showUrlValidationFeedback(JTextField urlField, String message, boolean warning) {
        urlField.putClientProperty(FlatClientProperties.OUTLINE,
                warning ? "warning" : FlatClientProperties.OUTLINE_ERROR);
        urlField.setToolTipText(message);
        SwingUtilities.invokeLater(() -> {
            urlField.requestFocusInWindow();
            urlField.selectAll();
        });
    }
}
