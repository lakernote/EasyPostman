package com.laker.postman.common.window;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.startup.StartupCoordinator;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.Serial;

/**
 * 启动欢迎窗口（Splash Window），用于主程序加载时的过渡。
 * 使用无边框 JFrame 代替 JWindow，降低 macOS 上首次创建顶层窗口的额外开销。
 */
@Slf4j
public class SplashWindow extends JFrame {
    @Serial
    private static final long serialVersionUID = 1L; // 添加序列化ID
    public static final int MIN_TIME = 350; // 最小显示时间，避免闪屏但不过度拖慢启动
    private static final float FADE_STEP = 0.08f; // 渐隐步长
    private static final float MIN_OPACITY = 0.05f; // 最小透明度
    private static final int FADE_TIMER_DELAY = 15; // 渐隐定时器延迟

    private JLabel statusLabel; // 状态标签，用于显示加载状态
    private transient Timer fadeOutTimer; // 渐隐计时器
    private transient ActionListener fadeOutListener; // 渐隐监听器，用于防止内存泄漏
    private volatile boolean isDisposed = false; // 标记窗口是否已释放

    public SplashWindow() {
        super();
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * 在 EDT 线程中初始化 UI 组件。
     */
    public void init() {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                initUI();
            } else {
                SwingUtilities.invokeAndWait(this::initUI);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            log.error("初始化 SplashWindow 被中断", e);
            dispose();
            throw new SplashWindowInitializationException("Failed to initialize splash window", e);
        } catch (Exception e) {
            log.error("初始化 SplashWindow 失败", e);
            // 如果初始化失败，确保窗口被正确释放
            dispose();
            throw new SplashWindowInitializationException("Failed to initialize splash window", e);
        }
    }

    /**
     * 初始化 UI 组件
     */
    private void initUI() {
        JPanel content = createContentPanel();
        initializeWindow(content);
    }

    /**
     * 创建主要内容面板
     */
    private JPanel createContentPanel() {
        JPanel content = getJPanel();

        content.add(createLogoPanel(), BorderLayout.CENTER);

        content.add(createInfoPanel(), BorderLayout.NORTH);

        content.add(createStatusPanel(), BorderLayout.SOUTH);
        return content;
    }

    /**
     * 创建Logo面板
     */
    private JPanel createLogoPanel() {
        // 创建容器面板，用于绘制圆形背景
        JPanel logoContainer = new JPanel() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 100; // 缩小圆形尺寸
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // 绘制外层光晕
                g2.setColor(getDecorativeDotColor(25));
                g2.fillOval(x - 6, y - 6, size + 12, size + 12);

                // 绘制中层光晕
                g2.setColor(getDecorativeDotColor(40));
                g2.fillOval(x - 4, y - 4, size + 8, size + 8);

                // 绘制圆形背景
                g2.setColor(getDecorativeDotColor(95));
                g2.fillOval(x, y, size, size);

                // 绘制内部微妙阴影，增加立体感
                g2.setColor(getDecorativeDotColor(15));
                g2.fillOval(x + 2, y + 2, size - 4, size - 4);

                // 绘制边框高光
                g2.setColor(isDarkTheme() ? new Color(255, 255, 255, 120) : Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);

                g2.dispose();
            }
        };
        logoContainer.setOpaque(false);
        logoContainer.setLayout(new BorderLayout());
        // 调整容器尺寸（100 + 12 = 112，设为 120 留边距）
        logoContainer.setPreferredSize(new Dimension(120, 120));

        Image scaledImage = Icons.LOGO.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        ImageIcon logoIcon = new ImageIcon(scaledImage);
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoContainer.add(logoLabel, BorderLayout.CENTER);

        return logoContainer;
    }

    /**
     * 创建应用信息面板
     */
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        infoPanel.setOpaque(false);

        // 应用名称
        JLabel appNameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.APP_NAME), SwingConstants.CENTER);
        appNameLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 8));
        appNameLabel.setForeground(getTextColor());
        infoPanel.add(appNameLabel);

        // 版本号
        JLabel versionLabel = new JLabel(SystemUtil.getCurrentVersion(), SwingConstants.CENTER);
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 4));
        versionLabel.setForeground(getTextColor());
        infoPanel.add(versionLabel);

        return infoPanel;
    }

    /**
     * 创建状态面板
     */
    private JPanel createStatusPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 3));
        bottomPanel.setOpaque(false);
        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SPLASH_STATUS_STARTING), SwingConstants.CENTER);
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 3)); // 比标准字体大3号
        statusLabel.setForeground(getTextColor());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        return bottomPanel;
    }

    /**
     * 初始化窗口
     */
    private void initializeWindow(JPanel content) {
        setContentPane(content);
        setSize(340, 250); // 设置窗口大小
        setLocationRelativeTo(null);  // 居中显示

        setupWindowProperties();

        setVisible(true); // 显示窗口
    }

    /**
     * 安全设置窗口属性
     */
    private void setupWindowProperties() {
        try {
            setBackground(new Color(0, 0, 0, 0)); // 透明背景
        } catch (Exception e) {
            log.warn("设置透明背景失败，使用默认背景", e);
        }

        try {
            setAlwaysOnTop(true); // 窗口总在最上层
        } catch (Exception e) {
            log.warn("设置窗口置顶失败", e);
        }
    }

    private static JPanel getJPanel() {
        JPanel content = new JPanel() { // 自定义面板，绘制渐变背景和圆角
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                boolean isDark = FlatLaf.isLafDark();

                // 主题适配的渐变背景，与主题背景色 (60, 63, 65) 协调
                Color gradientStart = isDark ? new Color(65, 67, 69) : ModernColors.PRIMARY;
                Color gradientEnd = isDark ? new Color(55, 57, 59) : ModernColors.PRIMARY_LIGHTER;
                GradientPaint gp = new GradientPaint(
                        0, 0, gradientStart,
                        getWidth(), getHeight(), gradientEnd
                );
                g2d.setPaint(gp);
                // 圆角背景
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);

                // 添加微妙的光泽效果
                Color highlightStart = isDark ? new Color(255, 255, 255, 8) : ModernColors.whiteWithAlpha(40);
                Color highlightEnd = isDark ? new Color(255, 255, 255, 0) : ModernColors.whiteWithAlpha(0);
                GradientPaint glossPaint = new GradientPaint(
                        0, 0, highlightStart,
                        0, getHeight() / 2.0f, highlightEnd
                );
                g2d.setPaint(glossPaint);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 32, 32);

                // 添加边框高光，与主题背景 (60, 63, 65) 协调
                Color borderColor = isDark ? new Color(75, 77, 80, 100) : ModernColors.whiteWithAlpha(80);
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 32, 32);

                g2d.dispose();
            }
        };
        content.setLayout(new BorderLayout(0, 10)); // 使用 BorderLayout 布局
        content.setOpaque(false); // 设置透明背景
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24)); // 设置内边距
        return content;
    }

    public void setStatus(String statusKey) {
        if (!isDisposed) {
            SwingUtilities.invokeLater(() -> updateStatusLabel(statusKey));
        }
    }

    /**
     * 更新状态标签
     */
    private void updateStatusLabel(String statusKey) {
        if (!isDisposed && statusLabel != null) {
            statusLabel.setText(I18nUtil.getMessage(statusKey));
        }
    }

    public void initMainFrame(StartupCoordinator startupCoordinator) {
        SwingWorker<MainFrame, String> worker = new SwingWorker<>() {
            @Override
            protected MainFrame doInBackground() {
                long start = System.currentTimeMillis();

                try {
                    MainFrame mainFrame = startupCoordinator.prepareMainFrame(stage -> {
                        switch (stage) {
                            case STARTING -> {
                                publish(MessageKeys.SPLASH_STATUS_STARTING);
                                setProgress(10);
                            }
                            case LOADING_PLUGINS -> {
                                publish(MessageKeys.SPLASH_STATUS_LOADING_PLUGINS);
                                setProgress(20);
                            }
                            case LOADING_MAIN -> {
                                publish(MessageKeys.SPLASH_STATUS_LOADING_MAIN);
                                setProgress(45);
                            }
                            case READY -> {
                                publish(MessageKeys.SPLASH_STATUS_READY);
                                setProgress(100);
                            }
                        }
                    });
                    ensureMinimumDisplayTime(start);
                    return mainFrame;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to prepare main frame", e);
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // 在EDT中更新状态
                if (!chunks.isEmpty() && !isDisposed) {
                    setStatus(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    if (isDisposed) {
                        return;
                    }

                    setStatus(MessageKeys.SPLASH_STATUS_DONE);
                    MainFrame mainFrame = get();

                    // 启动渐隐动画关闭 SplashWindow
                    startFadeOutAnimation(mainFrame, startupCoordinator);

                } catch (Exception e) {
                    handleMainFrameLoadError(e);
                }
            }
        };
        worker.execute();
    }

    /**
     * 确保最小显示时间
     */
    private void ensureMinimumDisplayTime(long startTimeMillis) {
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        long remaining = MIN_TIME - elapsed;
        if (remaining <= 0) {
            return;
        }
        try {
            Thread.sleep(remaining);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            log.warn("Thread interrupted while sleeping", interruptedException);
        }
    }

    /**
     * 处理主窗口加载错误
     */
    private void handleMainFrameLoadError(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.error("加载主窗口失败", e);

        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                I18nUtil.getMessage(MessageKeys.SPLASH_ERROR_LOAD_MAIN),
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        ));
        System.exit(1);
    }

    /**
     * 启动渐隐动画
     */
    private void startFadeOutAnimation(MainFrame mainFrame, StartupCoordinator startupCoordinator) {
        if (isDisposed) return;

        // 在开始渐隐动画之前就显示主窗口，实现重叠效果
        startupCoordinator.showMainFrame(mainFrame);

        fadeOutListener = createFadeOutListener(startupCoordinator);
        fadeOutTimer = new Timer(FADE_TIMER_DELAY, fadeOutListener);
        fadeOutTimer.start();
    }

    /**
     * 创建渐隐监听器
     */
    private ActionListener createFadeOutListener(StartupCoordinator startupCoordinator) {
        return e -> {
            if (isDisposed) {
                stopFadeOutAnimation();
                return;
            }

            try {
                processFadeOutStep(startupCoordinator);
            } catch (Exception ex) {
                handleFadeOutError(ex);
            }
        };
    }

    /**
     * 处理渐隐步骤
     */
    private void processFadeOutStep(StartupCoordinator startupCoordinator) {
        float opacity = getOpacity();
        if (opacity > MIN_OPACITY) {
            setOpacity(Math.max(0f, opacity - FADE_STEP));
        } else {
            completeFadeOut(startupCoordinator);
        }
    }

    /**
     * 完成渐隐效果
     */
    private void completeFadeOut(StartupCoordinator startupCoordinator) {
        stopFadeOutAnimation();
        disposeSafely();
        startupCoordinator.scheduleBackgroundUpdateCheck();
    }

    /**
     * 处理渐隐错误
     */
    private void handleFadeOutError(Exception ex) {
        log.warn("渐隐动画执行失败，直接关闭窗口", ex);
        stopFadeOutAnimation();
        disposeSafely();
        // 主窗口在渐隐开始前就已经显示，这里不需要再显示
    }

    /**
     * 停止渐隐动画
     */
    private void stopFadeOutAnimation() {
        if (fadeOutTimer != null) {
            fadeOutTimer.stop();
            if (fadeOutListener != null) {
                fadeOutTimer.removeActionListener(fadeOutListener);
            }
            fadeOutTimer = null;
            fadeOutListener = null;
        }
    }

    /**
     * 安全释放资源
     */
    private void disposeSafely() {
        if (isDisposed) return;

        isDisposed = true;
        stopFadeOutAnimation();

        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
        });
    }

    /**
     * 重写dispose方法，确保资源完全释放
     */
    @Override
    public void dispose() {
        if (!isDisposed) {
            disposeSafely();
        } else {
            super.dispose();
        }
    }

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的文字颜色
     */
    private Color getTextColor() {
        // 暗色主题使用更亮的文字颜色，提升可读性
        return isDarkTheme() ? new Color(230, 230, 230) : ModernColors.whiteWithAlpha(220);
    }

    /**
     * 获取主题适配的装饰点颜色
     */
    private Color getDecorativeDotColor(int alpha) {
        // 暗色主题使用更柔和的透明度，并稍微提亮
        if (isDarkTheme()) {
            // 使用更高的基础亮度，但降低透明度，使其更柔和
            int adjustedAlpha = (int) (alpha * 0.6); // 降低到 60% 透明度
            return new Color(255, 255, 255, adjustedAlpha);
        }
        return ModernColors.whiteWithAlpha(alpha);
    }
}
