package com.laker.postman.panel.http.runtime;

import com.laker.postman.common.component.DownloadProgressDialog;
import com.laker.postman.http.runtime.interaction.HttpCallbackDispatcher;
import com.laker.postman.http.runtime.interaction.DownloadProgressSink;
import com.laker.postman.http.runtime.interaction.DownloadProgressSinkFactory;
import com.laker.postman.http.runtime.interaction.ResponseSizeLimitWarning;
import com.laker.postman.http.runtime.interaction.ResponseSizeLimitWarningSink;
import com.laker.postman.http.runtime.observation.HttpLifecycleLogSink;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

@UtilityClass
public class SwingHttpRuntimeInteractionAdapter {
    public static DownloadProgressSinkFactory downloadProgressSinkFactory() {
        if (GraphicsEnvironment.isHeadless()) {
            return DownloadProgressSinkFactory.noop();
        }
        return SwingDownloadProgressSink::new;
    }

    public static ResponseSizeLimitWarningSink responseSizeLimitWarningSink() {
        if (GraphicsEnvironment.isHeadless()) {
            return ResponseSizeLimitWarningSink.noop();
        }
        return warning -> SwingUtilities.invokeLater(() -> showResponseSizeLimitWarning(warning));
    }

    public static HttpLifecycleLogSink lifecycleLogSink() {
        if (GraphicsEnvironment.isHeadless()) {
            return HttpLifecycleLogSink.noop();
        }
        return (message, level) -> {
            try {
                ConsolePanel.appendLog(message, toConsoleLogType(level));
            } catch (RuntimeException ignored) {
                // Logging must not break protocol callbacks.
            }
        };
    }

    public static HttpCallbackDispatcher callbackDispatcher() {
        if (GraphicsEnvironment.isHeadless()) {
            return HttpCallbackDispatcher.direct();
        }
        return new HttpCallbackDispatcher() {
            @Override
            public boolean isDispatchThread() {
                return SwingUtilities.isEventDispatchThread();
            }

            @Override
            public void dispatch(Runnable action) {
                if (action != null) {
                    SwingUtilities.invokeLater(action);
                }
            }
        };
    }

    private static ConsolePanel.LogType toConsoleLogType(HttpLifecycleLogSink.Level level) {
        return switch (level) {
            case DEBUG -> ConsolePanel.LogType.DEBUG;
            case INFO -> ConsolePanel.LogType.INFO;
            case SUCCESS -> ConsolePanel.LogType.SUCCESS;
            case WARN -> ConsolePanel.LogType.WARN;
            case ERROR -> ConsolePanel.LogType.ERROR;
        };
    }

    private static void showResponseSizeLimitWarning(ResponseSizeLimitWarning warning) {
        JOptionPane.showMessageDialog(
                null,
                warning.kind() == ResponseSizeLimitWarning.Kind.TEXT
                        ? I18nUtil.getMessage(MessageKeys.TEXT_TOO_LARGE,
                        warning.contentLengthMegabytes(), warning.maxDownloadMegabytes())
                        : I18nUtil.getMessage(MessageKeys.BINARY_TOO_LARGE,
                        warning.contentLengthMegabytes(), warning.maxDownloadMegabytes()),
                I18nUtil.getMessage(MessageKeys.DOWNLOAD_LIMIT_TITLE),
                JOptionPane.WARNING_MESSAGE
        );
    }

    private static final class SwingDownloadProgressSink implements DownloadProgressSink {
        private final DownloadProgressDialog progressDialog =
                new DownloadProgressDialog(I18nUtil.getMessage(MessageKeys.DOWNLOAD_PROGRESS_TITLE));

        @Override
        public void start(int contentLength) {
            progressDialog.startDownload(contentLength);
        }

        @Override
        public boolean isCancelled() {
            return progressDialog.isCancelled();
        }

        @Override
        public void updateProgress(int bytesRead) {
            progressDialog.updateProgress(bytesRead);
        }

        @Override
        public void finish() {
            progressDialog.finishDownload();
        }
    }
}
