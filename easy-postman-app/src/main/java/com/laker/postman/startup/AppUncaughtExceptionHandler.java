package com.laker.postman.startup;

import com.laker.postman.frame.MainFrame;
import com.laker.postman.util.ExceptionUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

/**
 * 统一处理未捕获异常，避免入口类直接依赖通知 UI 细节。
 */
@Slf4j
class AppUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (ExceptionUtil.shouldIgnoreException(throwable)) {
            log.debug("Ignoring harmless exception in thread: {}", thread.getName(), throwable);
            return;
        }

        log.error("Uncaught exception in thread: {}", thread.getName(), throwable);
        if (GraphicsEnvironment.isHeadless()) {
            log.debug("Skipping uncaught exception UI notification in headless mode");
            return;
        }
        SwingUtilities.invokeLater(this::notifyUser);
    }

    private void notifyUser() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        String message = I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE);
        if (isMainFrameVisible()) {
            NotificationCenter.showError(message);
            return;
        }

        JOptionPane.showMessageDialog(
                null,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private boolean isMainFrameVisible() {
        for (Window window : Window.getWindows()) {
            if (window instanceof MainFrame && window.isShowing()) {
                return true;
            }
        }
        return false;
    }
}
