package com.laker.postman.startup;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.AppConstants;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.service.UpdateService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * 协调应用启动过程中与主窗口相关的初始化步骤。
 */
@Slf4j
public class StartupCoordinator {
    private static final int UPDATE_CHECK_DELAY_MS = 2000;
    private static final int STARTUP_SHELL_PAINT_FALLBACK_MS = 300;

    public MainFrame prepareMainFrame(StartupProgressListener progressListener) throws Exception {
        long prepareStartNanos = System.nanoTime();
        StartupDiagnostics.mark("prepareMainFrame started");
        notifyProgress(progressListener, StartupStage.STARTING);
        BeanFactory.init(AppConstants.BASE_PACKAGE);
        StartupDiagnostics.mark("BeanFactory initialized");

        notifyProgress(progressListener, StartupStage.LOADING_PLUGINS);
        PluginRuntime.initialize();
        StartupDiagnostics.mark("Plugin runtime initialized");

        notifyProgress(progressListener, StartupStage.LOADING_MAIN);
        MainFrame mainFrame = createAndInitializeMainFrameOnEdt();
        StartupDiagnostics.mark("MainFrame shell prepared in " + StartupDiagnostics.formatSince(prepareStartNanos));

        notifyProgress(progressListener, StartupStage.READY);
        return mainFrame;
    }

    public void showMainFrame(MainFrame mainFrame) {
        if (mainFrame == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            showMainFrameOnEdt(mainFrame);
            return;
        }
        SwingUtilities.invokeLater(() -> showMainFrameOnEdt(mainFrame));
    }

    public void scheduleBackgroundUpdateCheck() {
        Timer delayTimer = new Timer(UPDATE_CHECK_DELAY_MS, e -> {
            try {
                BeanFactory.getBean(UpdateService.class).checkUpdateOnStartup();
                log.debug("Background update check scheduled after main window became visible");
            } catch (Exception ex) {
                log.warn("Failed to start background update check", ex);
            }
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    public void whenMainFrameReady(MainFrame mainFrame,
                                   int holdDelayMs,
                                   MainFrameReadinessCallbacks callbacks,
                                   Runnable onReady,
                                   Consumer<Throwable> onFailure) {
        if (mainFrame == null || onReady == null) {
            return;
        }
        MainFrameReadinessCallbacks safeCallbacks = callbacks != null ? callbacks : MainFrameReadinessCallbacks.NO_OP;
        safeCallbacks.onWaiting();

        final boolean[] contentLoaded = {false};
        final boolean[] shellReady = {false};
        final boolean[] completionScheduled = {false};
        final Timer[] shellFallbackTimer = {null};
        final Timer[] holdTimer = {null};
        Consumer<Throwable> failStartup = throwable -> {
            if (completionScheduled[0]) {
                return;
            }
            completionScheduled[0] = true;
            if (shellFallbackTimer[0] != null && shellFallbackTimer[0].isRunning()) {
                shellFallbackTimer[0].stop();
            }
            if (holdTimer[0] != null && holdTimer[0].isRunning()) {
                holdTimer[0].stop();
            }
            safeCallbacks.onMainContentLoadFailed(throwable);
            if (onFailure != null) {
                onFailure.accept(throwable);
            }
        };
        Runnable tryComplete = () -> {
            if (!contentLoaded[0] || !shellReady[0] || completionScheduled[0]) {
                return;
            }
            completionScheduled[0] = true;
            if (holdDelayMs > 0) {
                safeCallbacks.onHoldScheduled(holdDelayMs);
                holdTimer[0] = new Timer(holdDelayMs, e -> {
                    safeCallbacks.onReady();
                    onReady.run();
                });
                holdTimer[0].setRepeats(false);
                holdTimer[0].start();
                return;
            }
            safeCallbacks.onReady();
            onReady.run();
        };
        Runnable markShellReady = () -> {
            if (shellReady[0]) {
                return;
            }
            shellReady[0] = true;
            if (shellFallbackTimer[0] != null && shellFallbackTimer[0].isRunning()) {
                shellFallbackTimer[0].stop();
            }
            tryComplete.run();
        };

        mainFrame.whenMainContentLoaded(() -> {
            contentLoaded[0] = true;
            safeCallbacks.onMainContentReady();
            tryComplete.run();
        });
        mainFrame.whenMainContentLoadFailed(failStartup);
        mainFrame.whenStartupShellPainted(() -> {
            safeCallbacks.onStartupShellPainted();
            markShellReady.run();
        });
        shellFallbackTimer[0] = new Timer(STARTUP_SHELL_PAINT_FALLBACK_MS, e -> {
            if (shellReady[0]) {
                return;
            }
            safeCallbacks.onStartupShellPaintFallback(STARTUP_SHELL_PAINT_FALLBACK_MS);
            markShellReady.run();
        });
        shellFallbackTimer[0].setRepeats(false);
        shellFallbackTimer[0].start();
    }

    private void notifyProgress(StartupProgressListener progressListener, StartupStage stage) {
        if (progressListener != null && stage != null) {
            progressListener.onStageChanged(stage);
        }
    }

    private MainFrame createAndInitializeMainFrameOnEdt() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return createMainFrame();
        }

        MainFrame[] mainFrameHolder = new MainFrame[1];
        Throwable[] errorHolder = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                mainFrameHolder[0] = createMainFrame();
            } catch (Throwable throwable) {
                errorHolder[0] = throwable;
            }
        });

        if (errorHolder[0] != null) {
            if (errorHolder[0] instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(errorHolder[0]);
        }
        return mainFrameHolder[0];
    }

    private MainFrame createMainFrame() {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        mainFrame.initComponents();
        return mainFrame;
    }

    private void showMainFrameOnEdt(MainFrame mainFrame) {
        StartupDiagnostics.mark("showMainFrameOnEdt before setVisible");
        mainFrame.setVisible(true);
        StartupDiagnostics.mark("showMainFrameOnEdt after setVisible");
        mainFrame.toFront();
        mainFrame.requestFocus();
        SwingUtilities.invokeLater(() -> {
            StartupDiagnostics.mark("showMainFrameOnEdt scheduling main content load");
            mainFrame.loadMainContentAsync();
        });
    }

    public enum StartupStage {
        STARTING,
        LOADING_PLUGINS,
        LOADING_MAIN,
        READY
    }

    @FunctionalInterface
    public interface StartupProgressListener {
        void onStageChanged(StartupStage stage);
    }

    public interface MainFrameReadinessCallbacks {
        MainFrameReadinessCallbacks NO_OP = new MainFrameReadinessCallbacks() {
        };

        default void onWaiting() {
        }

        default void onStartupShellPainted() {
        }

        default void onMainContentReady() {
        }

        default void onMainContentLoadFailed(Throwable throwable) {
        }

        default void onStartupShellPaintFallback(int waitMs) {
        }

        default void onHoldScheduled(int holdDelayMs) {
        }

        default void onReady() {
        }
    }
}
