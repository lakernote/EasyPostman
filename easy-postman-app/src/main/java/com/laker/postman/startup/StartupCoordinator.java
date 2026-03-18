package com.laker.postman.startup;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.AppConstants;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.service.UpdateService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * 协调应用启动过程中与主窗口相关的初始化步骤。
 */
@Slf4j
public class StartupCoordinator {
    private static final int UPDATE_CHECK_DELAY_MS = 2000;

    public MainFrame prepareMainFrame(StartupProgressListener progressListener) throws Exception {
        notifyProgress(progressListener, StartupStage.STARTING);
        BeanFactory.init(AppConstants.BASE_PACKAGE);

        notifyProgress(progressListener, StartupStage.LOADING_PLUGINS);
        PluginRuntime.initialize();

        notifyProgress(progressListener, StartupStage.LOADING_MAIN);
        MainFrame mainFrame = createAndInitializeMainFrameOnEdt();

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
        mainFrame.setVisible(true);
        mainFrame.toFront();
        mainFrame.requestFocus();
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
}
