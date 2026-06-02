package com.laker.postman.frame;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.animation.WindowSnapshotTransition;
import com.laker.postman.common.component.placeholder.StartupShellPlaceholderPanel;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.panel.MainPanel;
import com.laker.postman.panel.lifecycle.AppExitCoordinator;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.UserSettingsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * 主窗口类，继承自 JFrame。
 */
@Slf4j
public class MainFrame extends JFrame {

    // 防抖延迟时间（毫秒）
    private static final int DEBOUNCE_DELAY = 500;
    // 缓存字段，避免重复计算
    private transient Dimension cachedMinWindowSize;
    private final transient Dimension cachedScreenSize;

    // 防抖计时器（final 避免重复赋值）
    private final transient Timer saveStateTimer;
    private final transient WindowSnapshotTransition startupShellTransition;
    private final transient MainFrameStartupLifecycle startupLifecycle;
    private transient JPanel startupShellPanel;

    // 单例模式，确保只有一个实例
    private MainFrame() {
        super();
        // 初始化屏幕尺寸缓存（避免重复系统调用）
        cachedScreenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // 初始化防抖计时器（只创建一次，避免重复创建对象）
        saveStateTimer = new Timer(DEBOUNCE_DELAY, e -> saveWindowState());
        saveStateTimer.setRepeats(false);
        startupShellTransition = new WindowSnapshotTransition(this);
        startupLifecycle = new MainFrameStartupLifecycle();

        setName(I18nUtil.getMessage(MessageKeys.APP_NAME));
        setTitle(I18nUtil.getMessage(MessageKeys.APP_NAME));
        setIconImage(Icons.LOGO.getImage());
        MainWindowChrome.applyInitialDecorations(this);
    }

    public void initComponents() {
        setJMenuBar(UiSingletonFactory.getInstance(TopMenuBar.class));
        installStartupShell();

        // 设置最小窗口尺寸，防止窗口被拖得太小
        Dimension minSize = getMinWindowSize();
        setMinimumSize(minSize);
        if (startupShellPanel != null) {
            startupShellPanel.setPreferredSize(minSize);
        }

        initWindowSize();
        initWindowCloseListener();
        initWindowStateListener();

        // 如果没有保存的窗口状态，使用 pack() 自适应组件大小
        if (!UserSettingsUtil.hasWindowState()) {
            pack();
        }

        // 只有在非最大化状态下才居中窗口，避免最大化恢复路径上的额外跳变
        boolean isMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
        if (!isMaximized) {
            setLocationRelativeTo(null);
        }
    }

    public void loadMainContentAsync() {
        if (!startupLifecycle.markMainContentLoadRequested()) {
            return;
        }
        // 主内容延后到启动壳显示后加载，避免首次展示窗口时被完整工作区初始化阻塞。
        Runnable task = () -> {
            try {
                replaceContentWithStartupTransition(UiSingletonFactory.getInstance(MainPanel.class));
                startupShellPanel = null;
                startupLifecycle.markMainContentLoaded();
            } catch (Throwable throwable) {
                log.error("Failed to initialize main content", throwable);
                startupLifecycle.markMainContentLoadFailed(throwable);
            }
        };

        SwingUtilities.invokeLater(task);
    }

    private void installStartupShell() {
        startupShellPanel = createStartupShellPanel();
        setContentPane(startupShellPanel);
        MainWindowChrome.applyBackground(this);
    }

    public void refreshWindowChrome() {
        MainWindowChrome.refresh(this);
    }

    private JPanel createStartupShellPanel() {
        JPanel root = new StartupShellPlaceholderPanel() {
            private boolean firstPaintHandled;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!firstPaintHandled) {
                    firstPaintHandled = true;
                    startupLifecycle.markStartupShellPainted();
                }
            }
        };
        root.setOpaque(true);
        root.setBackground(ModernColors.getBackgroundColor());
        return root;
    }

    private void replaceContentWithStartupTransition(Container nextContentPane) {
        WindowSnapshotTransition.CapturedSnapshot capturedSnapshot = null;
        Container currentContentPane = getContentPane();
        if (currentContentPane instanceof JComponent contentComponent) {
            // 这里只保留基于 layeredPane 的纯绘制快照过渡。
            // 不再使用 glassPane 覆盖整窗，避免挡住底层分割条的 hover / resize cursor。
            capturedSnapshot = startupShellTransition.captureSnapshot(contentComponent);
        }
        setContentPane(nextContentPane);
        MainWindowChrome.applyBackground(this);
        revalidate();
        repaint();
        startupShellTransition.start(capturedSnapshot);
    }

    public void whenMainContentLoaded(Runnable callback) {
        startupLifecycle.whenMainContentLoaded(callback);
    }

    public void whenMainContentLoadFailed(Consumer<Throwable> callback) {
        startupLifecycle.whenMainContentLoadFailed(callback);
    }

    public void whenStartupShellPainted(Runnable callback) {
        startupLifecycle.whenStartupShellPainted(callback);
    }

    private void initWindowSize() {
        // 如果已有保存的窗口状态，则恢复上次的窗口状态
        if (UserSettingsUtil.hasWindowState()) {
            restoreWindowState();
            return;
        }

        // 设置默认窗口大小（使用缓存的屏幕尺寸）
        setSize(getMinWindowSize());

        // 小屏幕默认最大化
        if (MainWindowSizePolicy.shouldStartMaximized(cachedScreenSize.getWidth())) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    private void initWindowCloseListener() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 清理资源并保存状态
                cleanup();
                saveWindowState();
                BeanFactory.getBean(AppExitCoordinator.class).exitApplication();
            }

            @Override
            public void windowStateChanged(WindowEvent e) {
                // 窗口最大化/最小化状态改变时也使用防抖保存
                scheduleSaveWindowState();
            }
        });
    }

    private void initWindowStateListener() {
        // 监听窗口大小和位置变化，使用防抖机制避免频繁保存
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isVisible()) {
                    scheduleSaveWindowState();
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    scheduleSaveWindowState();
                }
            }
        });
    }

    /**
     * 延迟保存窗口状态（防抖）
     * 在用户拖动窗口或调整大小时，避免频繁写入磁盘
     */
    private void scheduleSaveWindowState() {
        // 使用预创建的 Timer，避免重复创建对象
        if (saveStateTimer.isRunning()) {
            saveStateTimer.restart();
        } else {
            saveStateTimer.start();
        }
    }

    private void saveWindowState() {
        try {
            boolean isMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            Dimension minSize = getMinWindowSize();

            int width;
            int height;
            if (!isMaximized) {
                Dimension size = getSize();
                width = Math.max(size.width, minSize.width);
                height = Math.max(size.height, minSize.height);
            } else {
                // 最大化时，保存上次的非最大化尺寸
                // 避免重复 I/O，如果已有保存值就复用
                if (UserSettingsUtil.hasWindowState()) {
                    Integer savedWidth = UserSettingsUtil.getWindowWidth();
                    Integer savedHeight = UserSettingsUtil.getWindowHeight();
                    width = (savedWidth != null && savedWidth > 0) ? Math.max(savedWidth, minSize.width) : minSize.width;
                    height = (savedHeight != null && savedHeight > 0) ? Math.max(savedHeight, minSize.height) : minSize.height;
                } else {
                    width = minSize.width;
                    height = minSize.height;
                }
            }

            // 保存完整的 extendedState，可以恢复更多的窗口状态（如部分最大化、最小化等）
            int extendedState = getExtendedState();
            UserSettingsUtil.saveWindowState(width, height, extendedState);
            log.debug("窗口状态已保存: width={}, height={}, extendedState={}", width, height, extendedState);
        } catch (Exception e) {
            log.warn("保存窗口状态失败", e);
        }
    }

    /**
     * 清理资源（在窗口关闭时调用）
     */
    private void cleanup() {
        // 停止窗口状态保存定时器
        if (saveStateTimer != null && saveStateTimer.isRunning()) {
            saveStateTimer.stop();
        }
        startupShellTransition.stop();

        // 清理性能测试面板资源（停止定时器等）
        try {
            UiSingletonFactory.getExistingInstance(PerformancePanel.class)
                    .ifPresent(PerformancePanel::cleanup);
        } catch (Exception e) {
            log.warn("清理 PerformancePanel 资源时出错: {}", e.getMessage());
        }
    }

    private void restoreWindowState() {
        try {
            if (UserSettingsUtil.hasWindowState()) {
                Integer width = UserSettingsUtil.getWindowWidth();
                Integer height = UserSettingsUtil.getWindowHeight();
                Integer extendedState = UserSettingsUtil.getWindowExtendedState();

                Dimension minSize = getMinWindowSize();
                int actualWidth = (width != null && width > 0) ? Math.max(width, minSize.width) : minSize.width;
                int actualHeight = (height != null && height > 0) ? Math.max(height, minSize.height) : minSize.height;
                int actualState = (extendedState != null) ? extendedState : Frame.NORMAL;
                boolean isMaximized = (actualState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;

                if (isMaximized) {
                    // 最大化状态时，先设置尺寸并居中，再设置 extendedState
                    // 这样当用户从最大化恢复时，窗口会出现在屏幕中央
                    setSize(actualWidth, actualHeight);
                    setLocationRelativeTo(null);  // 居中窗口
                    setExtendedState(actualState);
                } else {
                    // 非最大化状态：正常设置尺寸和状态
                    setSize(new Dimension(actualWidth, actualHeight));
                    setExtendedState(actualState);
                }

                log.debug("窗口状态已恢复: width={}, height={}, extendedState={}",
                        actualWidth, actualHeight, actualState);
            }
        } catch (Exception e) {
            log.warn("恢复窗口状态失败", e);
        }
    }

    private Dimension getMinWindowSize() {
        // 使用缓存避免重复计算
        if (cachedMinWindowSize != null) {
            return cachedMinWindowSize;
        }

        cachedMinWindowSize = MainWindowSizePolicy.minimumSizeForScreenWidth(cachedScreenSize.getWidth());

        log.debug("计算最小窗口尺寸: {}x{} (屏幕: {}x{})",
                cachedMinWindowSize.width, cachedMinWindowSize.height,
                (int) cachedScreenSize.getWidth(), (int) cachedScreenSize.getHeight());

        return cachedMinWindowSize;
    }
}
