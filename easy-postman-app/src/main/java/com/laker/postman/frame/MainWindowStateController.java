package com.laker.postman.frame;

import com.laker.postman.util.UserSettingsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 管理主窗口尺寸、位置和最大化状态的保存与恢复。
 */
@Slf4j
class MainWindowStateController {
    private static final int SAVE_DEBOUNCE_DELAY_MS = 500;

    private final JFrame frame;
    private final Dimension cachedScreenSize;
    private final Timer saveStateTimer;
    private Dimension cachedMinWindowSize;

    MainWindowStateController(JFrame frame) {
        this.frame = frame;
        cachedScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        saveStateTimer = new Timer(SAVE_DEBOUNCE_DELAY_MS, e -> saveWindowState());
        saveStateTimer.setRepeats(false);
    }

    Dimension getMinWindowSize() {
        if (cachedMinWindowSize != null) {
            return cachedMinWindowSize;
        }

        cachedMinWindowSize = MainWindowSizePolicy.minimumSizeForScreenWidth(cachedScreenSize.getWidth());
        log.debug("计算最小窗口尺寸: {}x{} (屏幕: {}x{})",
                cachedMinWindowSize.width, cachedMinWindowSize.height,
                (int) cachedScreenSize.getWidth(), (int) cachedScreenSize.getHeight());
        return cachedMinWindowSize;
    }

    void initWindowSize() {
        if (hasSavedWindowState()) {
            restoreWindowState();
            return;
        }

        frame.setSize(getMinWindowSize());
        if (MainWindowSizePolicy.shouldStartMaximized(cachedScreenSize.getWidth())) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    boolean hasSavedWindowState() {
        return UserSettingsUtil.hasWindowState();
    }

    void installStateListeners() {
        frame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                scheduleSaveWindowState();
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scheduleSaveWindowStateIfVisible();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                scheduleSaveWindowStateIfVisible();
            }
        });
    }

    void scheduleSaveWindowState() {
        if (saveStateTimer.isRunning()) {
            saveStateTimer.restart();
        } else {
            saveStateTimer.start();
        }
    }

    void saveWindowState() {
        try {
            boolean isMaximized = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            Dimension minSize = getMinWindowSize();

            int width;
            int height;
            if (isMaximized) {
                width = savedWindowWidthOrDefault(minSize);
                height = savedWindowHeightOrDefault(minSize);
            } else {
                Dimension size = frame.getSize();
                width = Math.max(size.width, minSize.width);
                height = Math.max(size.height, minSize.height);
            }

            int extendedState = frame.getExtendedState();
            UserSettingsUtil.saveWindowState(width, height, extendedState);
            log.debug("窗口状态已保存: width={}, height={}, extendedState={}", width, height, extendedState);
        } catch (Exception e) {
            log.warn("保存窗口状态失败", e);
        }
    }

    void stop() {
        if (saveStateTimer.isRunning()) {
            saveStateTimer.stop();
        }
    }

    private void scheduleSaveWindowStateIfVisible() {
        if (frame.isVisible()) {
            scheduleSaveWindowState();
        }
    }

    private int savedWindowWidthOrDefault(Dimension minSize) {
        if (!hasSavedWindowState()) {
            return minSize.width;
        }
        Integer savedWidth = UserSettingsUtil.getWindowWidth();
        return (savedWidth != null && savedWidth > 0) ? Math.max(savedWidth, minSize.width) : minSize.width;
    }

    private int savedWindowHeightOrDefault(Dimension minSize) {
        if (!hasSavedWindowState()) {
            return minSize.height;
        }
        Integer savedHeight = UserSettingsUtil.getWindowHeight();
        return (savedHeight != null && savedHeight > 0) ? Math.max(savedHeight, minSize.height) : minSize.height;
    }

    private void restoreWindowState() {
        try {
            Integer width = UserSettingsUtil.getWindowWidth();
            Integer height = UserSettingsUtil.getWindowHeight();
            Integer extendedState = UserSettingsUtil.getWindowExtendedState();

            Dimension minSize = getMinWindowSize();
            int actualWidth = (width != null && width > 0) ? Math.max(width, minSize.width) : minSize.width;
            int actualHeight = (height != null && height > 0) ? Math.max(height, minSize.height) : minSize.height;
            int actualState = (extendedState != null) ? extendedState : Frame.NORMAL;

            if ((actualState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                frame.setSize(actualWidth, actualHeight);
                frame.setLocationRelativeTo(null);
                frame.setExtendedState(actualState);
            } else {
                frame.setSize(new Dimension(actualWidth, actualHeight));
                frame.setExtendedState(actualState);
            }

            log.debug("窗口状态已恢复: width={}, height={}, extendedState={}",
                    actualWidth, actualHeight, actualState);
        } catch (Exception e) {
            log.warn("恢复窗口状态失败", e);
        }
    }
}
