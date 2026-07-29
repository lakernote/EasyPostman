package com.laker.postman.startup;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.ExecutionException;

/**
 * 启动失败的统一出口：记录错误、提示用户并退出进程。
 */
@Slf4j
@UtilityClass
public class StartupFailureHandler {

    public void showStartupErrorAndExit(Throwable throwable) {
        showErrorAndExit(
                throwable,
                "Failed to start application",
                MessageKeys.SPLASH_ERROR_LOAD_MAIN
        );
    }

    public void showSingleInstanceSetupErrorAndExit(Throwable throwable) {
        showErrorAndExit(
                throwable,
                "Failed to initialize GUI single-instance coordination",
                MessageKeys.STARTUP_SINGLE_INSTANCE_SETUP_FAILED
        );
    }

    public void showExistingInstanceUnavailableAndExit() {
        showErrorAndExit(
                null,
                "An existing GUI instance holds the lock but could not be activated",
                MessageKeys.STARTUP_SINGLE_INSTANCE_UNREACHABLE
        );
    }

    private void showErrorAndExit(Throwable throwable, String logMessage, String messageKey) {
        if (throwable == null) {
            log.warn(logMessage);
        } else {
            log.error(logMessage, unwrap(throwable));
        }

        Runnable showErrorAndExit = () -> {
            if (!GraphicsEnvironment.isHeadless()) {
                try {
                    showSwingStartupError(messageKey);
                } catch (Exception e) {
                    log.warn("Failed to show startup error dialog", e);
                }
            }
            System.exit(1);
        };

        if (GraphicsEnvironment.isHeadless() || SwingUtilities.isEventDispatchThread()) {
            showErrorAndExit.run();
            return;
        }
        SwingUtilities.invokeLater(showErrorAndExit);
    }

    private void showSwingStartupError(String messageKey) {
        JOptionPane.showMessageDialog(
                null,
                I18nUtil.getMessage(messageKey),
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof ExecutionException executionException && executionException.getCause() != null) {
            return executionException.getCause();
        }
        return throwable;
    }
}
