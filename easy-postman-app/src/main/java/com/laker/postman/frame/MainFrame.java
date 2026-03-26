package com.laker.postman.frame;

import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.panel.MainPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.service.ExitService;
import com.laker.postman.startup.StartupDiagnostics;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 主窗口类，继承自 JFrame。
 */
@Slf4j
public class MainFrame extends JFrame {

    // 屏幕尺寸常量
    private static final int SCREEN_WIDTH_4K = 3840;
    private static final int SCREEN_WIDTH_2K = 2560;
    private static final int SCREEN_WIDTH_FHD = 1920;
    private static final int SCREEN_WIDTH_HD = 1280;
    private static final int SCREEN_WIDTH_THRESHOLD = 1366;

    // 窗口尺寸常量（增大默认尺寸，适应现代屏幕）
    private static final int MIN_WIDTH_4K = 1920;
    private static final int MIN_HEIGHT_4K = 1200;
    private static final int MIN_WIDTH_2K = 1600;
    private static final int MIN_HEIGHT_2K = 1000;
    private static final int MIN_WIDTH_FHD = 1400;
    private static final int MIN_HEIGHT_FHD = 900;
    private static final int MIN_WIDTH_HD = 1280;
    private static final int MIN_HEIGHT_HD = 800;
    private static final int MIN_WIDTH_WXGA = 1100;
    private static final int MIN_HEIGHT_WXGA = 700;

    // 防抖延迟时间（毫秒）
    private static final int DEBOUNCE_DELAY = 500;

    // 缓存字段，避免重复计算
    private transient Dimension cachedMinWindowSize;
    private final transient Dimension cachedScreenSize;

    // 防抖计时器（final 避免重复赋值）
    private final transient Timer saveStateTimer;
    private transient JPanel startupShellPanel;
    private transient volatile boolean mainContentLoaded;
    private transient volatile boolean mainContentLoadRequested;
    private transient volatile Throwable mainContentLoadFailure;
    private transient volatile boolean startupShellPainted;
    private final transient List<Runnable> mainContentLoadedCallbacks = new ArrayList<>();
    private final transient List<Consumer<Throwable>> mainContentLoadFailedCallbacks = new ArrayList<>();
    private final transient List<Runnable> startupShellPaintedCallbacks = new ArrayList<>();

    // 单例模式，确保只有一个实例
    private MainFrame() {
        super();
        // 初始化屏幕尺寸缓存（避免重复系统调用）
        cachedScreenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // 初始化防抖计时器（只创建一次，避免重复创建对象）
        saveStateTimer = new Timer(DEBOUNCE_DELAY, e -> saveWindowState());
        saveStateTimer.setRepeats(false);

        setName(I18nUtil.getMessage(MessageKeys.APP_NAME));
        setTitle(I18nUtil.getMessage(MessageKeys.APP_NAME));
        setIconImage(Icons.LOGO.getImage());
        applyWindowBackground();

        // macOS 标题栏扩展属性要在首次显示前设置，否则首次显示后再补设置容易出现抖动。
        applyMacWindowDecorations();
        applyMacWindowAppearance();
    }

    public void initComponents() {
        long initStartNanos = System.nanoTime();
        setJMenuBar(SingletonFactory.getInstance(TopMenuBar.class));
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
        StartupDiagnostics.mark("MainFrame shell initialized in " + StartupDiagnostics.formatSince(initStartNanos));
    }

    public void loadMainContentAsync() {
        if (mainContentLoaded || mainContentLoadRequested) {
            return;
        }
        mainContentLoadRequested = true;
        Runnable task = () -> {
            if (mainContentLoaded) {
                return;
            }
            long loadStartNanos = System.nanoTime();
            try {
                StartupDiagnostics.mark("MainFrame content load started");
                setContentPane(SingletonFactory.getInstance(MainPanel.class));
                revalidate();
                repaint();
                mainContentLoaded = true;
                notifyMainContentLoaded();
                StartupDiagnostics.mark("MainFrame content load finished in " + StartupDiagnostics.formatSince(loadStartNanos));
            } catch (Throwable throwable) {
                mainContentLoadFailure = throwable;
                log.error("Failed to initialize main content", throwable);
                StartupDiagnostics.mark("MainFrame content load failed after " + StartupDiagnostics.formatSince(loadStartNanos));
                notifyMainContentLoadFailed(throwable);
            }
        };

        SwingUtilities.invokeLater(task);
    }

    private void installStartupShell() {
        startupShellPanel = createStartupShellPanel();
        setContentPane(startupShellPanel);
        applyWindowBackground();
    }

    private void applyWindowBackground() {
        Color background = ModernColors.getBackgroundColor();
        setBackground(background);
        if (getRootPane() != null) {
            getRootPane().setOpaque(true);
            getRootPane().setBackground(background);
        }
        if (getLayeredPane() != null) {
            getLayeredPane().setOpaque(true);
            getLayeredPane().setBackground(background);
        }
        if (getGlassPane() instanceof JComponent glassPane) {
            glassPane.setOpaque(false);
            glassPane.setBackground(background);
        }
        Container contentPane = getContentPane();
        if (contentPane instanceof JComponent contentComponent) {
            contentComponent.setOpaque(true);
            contentComponent.setBackground(background);
        }
    }

    private void applyMacWindowAppearance() {
        if (!SystemInfo.isMacFullWindowContentSupported || getRootPane() == null) {
            return;
        }
        if (SimpleThemeManager.isDarkTheme()) {
            getRootPane().putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantDark");
        } else {
            getRootPane().putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantLight");
        }
    }

    private void applyMacWindowDecorations() {
        if (!SystemInfo.isMacFullWindowContentSupported || getRootPane() == null) {
            return;
        }
        // 在窗口首显前一次性应用，避免显示后再切换 title bar / fullWindowContent 造成二次重绘。
        getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
        StartupDiagnostics.mark("Applied macOS window decorations before first show");
    }

    private JPanel createStartupShellPanel() {
        JPanel root = new JPanel(new BorderLayout()) {
            private boolean firstPaintHandled;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!firstPaintHandled) {
                    firstPaintHandled = true;
                    startupShellPainted = true;
                    StartupDiagnostics.mark("Startup shell first paint");
                    notifyStartupShellPainted();
                }
            }
        };
        root.setOpaque(true);
        root.setBackground(ModernColors.getBackgroundColor());
        return root;
    }

    public void whenMainContentLoaded(Runnable callback) {
        if (callback == null) {
            return;
        }
        if (mainContentLoaded) {
            SwingUtilities.invokeLater(callback);
            return;
        }
        synchronized (mainContentLoadedCallbacks) {
            if (mainContentLoaded) {
                SwingUtilities.invokeLater(callback);
            } else {
                mainContentLoadedCallbacks.add(callback);
            }
        }
    }

    public void whenMainContentLoadFailed(Consumer<Throwable> callback) {
        if (callback == null) {
            return;
        }
        if (mainContentLoadFailure != null) {
            Throwable failure = mainContentLoadFailure;
            SwingUtilities.invokeLater(() -> callback.accept(failure));
            return;
        }
        synchronized (mainContentLoadFailedCallbacks) {
            if (mainContentLoadFailure != null) {
                Throwable failure = mainContentLoadFailure;
                SwingUtilities.invokeLater(() -> callback.accept(failure));
            } else {
                mainContentLoadFailedCallbacks.add(callback);
            }
        }
    }

    public void whenStartupShellPainted(Runnable callback) {
        if (callback == null) {
            return;
        }
        if (startupShellPainted) {
            SwingUtilities.invokeLater(callback);
            return;
        }
        synchronized (startupShellPaintedCallbacks) {
            if (startupShellPainted) {
                SwingUtilities.invokeLater(callback);
            } else {
                startupShellPaintedCallbacks.add(callback);
            }
        }
    }

    private void notifyMainContentLoaded() {
        List<Runnable> callbacksToRun;
        synchronized (mainContentLoadedCallbacks) {
            callbacksToRun = new ArrayList<>(mainContentLoadedCallbacks);
            mainContentLoadedCallbacks.clear();
        }
        for (Runnable callback : callbacksToRun) {
            SwingUtilities.invokeLater(callback);
        }
    }

    private void notifyMainContentLoadFailed(Throwable throwable) {
        List<Consumer<Throwable>> callbacksToRun;
        synchronized (mainContentLoadFailedCallbacks) {
            callbacksToRun = new ArrayList<>(mainContentLoadFailedCallbacks);
            mainContentLoadFailedCallbacks.clear();
        }
        for (Consumer<Throwable> callback : callbacksToRun) {
            SwingUtilities.invokeLater(() -> callback.accept(throwable));
        }
    }

    private void notifyStartupShellPainted() {
        List<Runnable> callbacksToRun;
        synchronized (startupShellPaintedCallbacks) {
            callbacksToRun = new ArrayList<>(startupShellPaintedCallbacks);
            startupShellPaintedCallbacks.clear();
        }
        for (Runnable callback : callbacksToRun) {
            SwingUtilities.invokeLater(callback);
        }
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
        if (cachedScreenSize.getWidth() <= SCREEN_WIDTH_THRESHOLD) {
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
                BeanFactory.getBean(ExitService.class).exit();
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

        // 清理性能测试面板资源（停止定时器等）
        try {
            PerformancePanel performancePanel =
                    SingletonFactory.getInstance(PerformancePanel.class);
            performancePanel.cleanup();
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

        // 使用缓存的屏幕尺寸，根据不同分辨率设置合理的窗口大小
        double screenWidth = cachedScreenSize.getWidth();

        if (screenWidth >= SCREEN_WIDTH_4K) {
            // 4K 及以上分辨率
            cachedMinWindowSize = new Dimension(MIN_WIDTH_4K, MIN_HEIGHT_4K);
        } else if (screenWidth >= SCREEN_WIDTH_2K) {
            // 2K 分辨率
            cachedMinWindowSize = new Dimension(MIN_WIDTH_2K, MIN_HEIGHT_2K);
        } else if (screenWidth >= SCREEN_WIDTH_FHD) {
            // Full HD 分辨率
            cachedMinWindowSize = new Dimension(MIN_WIDTH_FHD, MIN_HEIGHT_FHD);
        } else if (screenWidth >= SCREEN_WIDTH_HD) {
            // HD 分辨率
            cachedMinWindowSize = new Dimension(MIN_WIDTH_HD, MIN_HEIGHT_HD);
        } else {
            // 小屏幕
            cachedMinWindowSize = new Dimension(MIN_WIDTH_WXGA, MIN_HEIGHT_WXGA);
        }

        log.debug("计算最小窗口尺寸: {}x{} (屏幕: {}x{})",
                cachedMinWindowSize.width, cachedMinWindowSize.height,
                (int) cachedScreenSize.getWidth(), (int) cachedScreenSize.getHeight());

        return cachedMinWindowSize;
    }
}
