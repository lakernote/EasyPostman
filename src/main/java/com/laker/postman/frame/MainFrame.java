package com.laker.postman.frame;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.panel.MainPanel;
import com.laker.postman.service.ExitService;
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
    }

    public void initComponents() {
        setContentPane(SingletonFactory.getInstance(MainPanel.class));

        // 设置最小窗口尺寸，防止窗口被拖得太小
        Dimension minSize = getMinWindowSize();
        setMinimumSize(minSize);

        initWindowSize();
        initWindowCloseListener();
        initWindowStateListener();

        // 如果没有保存的窗口状态，使用 pack() 自适应组件大小
        if (!UserSettingsUtil.hasWindowState()) {
            pack();
        }

        // 只有在非最大化状态下才居中窗口，避免全屏时的闪烁
        boolean isMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
        if (!isMaximized) {
            setLocationRelativeTo(null);
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
                ExitService.exit();
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

            UserSettingsUtil.saveWindowState(width, height, isMaximized);
            log.debug("窗口状态已保存: width={}, height={}, maximized={}", width, height, isMaximized);
        } catch (Exception e) {
            log.warn("保存窗口状态失败", e);
        }
    }

    /**
     * 清理资源（在窗口关闭时调用）
     */
    private void cleanup() {
        if (saveStateTimer != null && saveStateTimer.isRunning()) {
            saveStateTimer.stop();
        }
    }

    private void restoreWindowState() {
        try {
            if (UserSettingsUtil.hasWindowState()) {
                Integer width = UserSettingsUtil.getWindowWidth();
                Integer height = UserSettingsUtil.getWindowHeight();
                boolean isMaximized = UserSettingsUtil.isWindowMaximized();

                Dimension minSize = getMinWindowSize();
                int actualWidth = (width != null && width > 0) ? Math.max(width, minSize.width) : minSize.width;
                int actualHeight = (height != null && height > 0) ? Math.max(height, minSize.height) : minSize.height;

                if (isMaximized) {
                    // 最大化状态：先设置尺寸（用于退出最大化时的尺寸），但不显示，然后直接设置最大化
                    // 这样可以避免窗口先显示为普通尺寸再最大化的闪烁
                    setBounds(0, 0, actualWidth, actualHeight);
                    setExtendedState(Frame.MAXIMIZED_BOTH);
                } else {
                    // 非最大化状态：正常设置尺寸
                    setSize(new Dimension(actualWidth, actualHeight));
                }

                log.debug("窗口状态已恢复: width={}, height={}, maximized={}",
                        actualWidth, actualHeight, isMaximized);
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
                (int)cachedScreenSize.getWidth(), (int)cachedScreenSize.getHeight());

        return cachedMinWindowSize;
    }
}