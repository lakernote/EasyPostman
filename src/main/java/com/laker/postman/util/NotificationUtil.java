package com.laker.postman.util;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Toast 风格的自动关闭通知工具类
 *
 * <h2>功能特性</h2>
 * <ul>
 *   <li>支持 4 种通知类型：成功、信息、警告、错误</li>
 *   <li>自动淡入淡出动画，流畅的滑入效果</li>
 *   <li>支持长文本智能展开/收起</li>
 *   <li>鼠标悬停暂停自动关闭</li>
 *   <li>点击复制内容到剪贴板</li>
 *   <li>可手动关闭（可选）</li>
 *   <li>进度条显示剩余时间（可配置）</li>
 *   <li>支持多个通知堆叠显示</li>
 *   <li>深色/浅色主题自适应</li>
 * </ul>
 *
 * <h2>基本用法</h2>
 * <pre>{@code
 * // 成功通知（2秒后自动关闭）
 * NotificationUtil.showSuccess("操作成功完成！");
 *
 * // 信息通知（3秒后自动关闭）
 * NotificationUtil.showInfo("这是一条信息提示");
 *
 * // 警告通知（3秒后自动关闭）
 * NotificationUtil.showWarning("请注意：这是一个警告");
 *
 * // 错误通知（4秒后自动关闭）
 * NotificationUtil.showError("操作失败，请重试");
 * }</pre>
 *
 * <h2>高级用法</h2>
 * <pre>{@code
 * // 长文本通知（支持展开/收起，5秒后自动关闭）
 * String longMessage = "这是第一行\n这是第二行\n这是第三行\n这是第四行";
 * NotificationUtil.showLongMessage(longMessage, NotificationType.INFO);
 *
 * // 可手动关闭的通知（带关闭按钮，10秒后自动关闭）
 * NotificationUtil.showCloseable("重要通知", NotificationType.WARNING, 10);
 *
 * // 自定义位置的通知
 * NotificationUtil.showToast(
 *     "消息内容",
 *     NotificationType.INFO,
 *     3,  // 持续时间（秒）
 *     NotificationPosition.TOP_CENTER  // 位置
 * );
 * }</pre>
 *
 * <h2>配置选项</h2>
 * <pre>{@code
 * // 全局关闭进度条显示
 * NotificationUtil.setShowProgressBar(false);
 *
 * // 开启进度条显示（默认）
 * NotificationUtil.setShowProgressBar(true);
 * }</pre>
 *
 * <h2>通知位置</h2>
 * <ul>
 *   <li>{@link NotificationPosition#TOP_RIGHT TOP_RIGHT} - 右上角（默认，推荐）</li>
 *   <li>{@link NotificationPosition#TOP_CENTER TOP_CENTER} - 顶部居中</li>
 *   <li>{@link NotificationPosition#TOP_LEFT TOP_LEFT} - 左上角</li>
 *   <li>{@link NotificationPosition#BOTTOM_RIGHT BOTTOM_RIGHT} - 右下角</li>
 *   <li>{@link NotificationPosition#BOTTOM_CENTER BOTTOM_CENTER} - 底部居中</li>
 *   <li>{@link NotificationPosition#BOTTOM_LEFT BOTTOM_LEFT} - 左下角</li>
 *   <li>{@link NotificationPosition#CENTER CENTER} - 屏幕中央</li>
 * </ul>
 *
 * <h2>交互说明</h2>
 * <ul>
 *   <li><b>鼠标悬停</b>：暂停自动关闭倒计时，显示高亮边框</li>
 *   <li><b>单击</b>：长文本展开/收起，短文本复制到剪贴板</li>
 *   <li><b>双击</b>：复制完整内容到剪贴板</li>
 *   <li><b>关闭按钮</b>：立即关闭通知（仅 showCloseable 方法）</li>
 * </ul>
 *
 * <h2>使用建议</h2>
 * <ul>
 *   <li>短消息（&lt;50字）：使用 showSuccess/Info/Warning/Error</li>
 *   <li>中等消息（50-150字）：使用 showCloseable，增加持续时间</li>
 *   <li>长消息（&gt;150字或多行）：使用 showLongMessage</li>
 *   <li>重要提醒：使用 TOP_CENTER 位置 + WARNING/ERROR 类型</li>
 *   <li>后台操作反馈：使用 TOP_RIGHT 位置（默认）</li>
 * </ul>
 *
 * <h2>注意事项</h2>
 * <ul>
 *   <li>通知会自动堆叠显示，避免重叠</li>
 *   <li>悬停时暂停的通知，移开鼠标后会继续倒计时</li>
 *   <li>所有方法都是线程安全的，可在任何线程调用</li>
 *   <li>建议在 EDT（Event Dispatch Thread）中调用以获得最佳性能</li>
 * </ul>
 *
 * @author laker
 * @since 1.0
 * @see NotificationType
 * @see NotificationPosition
 */
public class NotificationUtil {

    private NotificationUtil() {
        // 工具类，隐藏构造函数
    }

    /**
     * 获取 MainFrame 实例
     */
    private static Window getMainFrame() {
        try {
            return SingletonFactory.getInstance(MainFrame.class);
        } catch (Exception e) {
            return JOptionPane.getRootFrame();
        }
    }

    // 通知类型枚举
    public enum NotificationType {
        SUCCESS(ModernColors.SUCCESS, "✓"),      // 绿色 - 成功
        INFO(ModernColors.INFO, "ℹ"),            // 蓝色 - 信息
        WARNING(ModernColors.WARNING, "⚠"),      // 橙色 - 警告
        ERROR(ModernColors.ERROR, "✕");          // 红色 - 错误

        private final Color color;
        private final String icon;

        NotificationType(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public Color getColor() {
            return color;
        }

        public String getIcon() {
            return icon;
        }
    }

    // 通知位置枚举
    public enum NotificationPosition {
        TOP_RIGHT,
        TOP_CENTER,
        TOP_LEFT,
        BOTTOM_RIGHT,
        BOTTOM_CENTER,
        BOTTOM_LEFT,
        CENTER
    }

    // 默认位置
    private static NotificationPosition defaultPosition = NotificationPosition.TOP_RIGHT;

    // 是否显示进度条（默认显示）
    private static boolean showProgressBar = true;

    // 当前显示的通知列表（用于堆叠管理）
    private static final List<ToastWindow> activeToasts = new ArrayList<>();

    /**
     * 设置是否显示进度条
     */
    public static void setShowProgressBar(boolean show) {
        showProgressBar = show;
    }


    /**
     * 显示成功通知（2秒后自动关闭）
     */
    public static void showSuccess(String message) {
        showToast(message, NotificationType.SUCCESS, 2);
    }

    /**
     * 显示信息通知（3秒后自动关闭）
     */
    public static void showInfo(String message) {
        showToast(message, NotificationType.INFO, 3);
    }

    /**
     * 显示警告通知（3秒后自动关闭）
     */
    public static void showWarning(String message) {
        showToast(message, NotificationType.WARNING, 3);
    }

    /**
     * 显示错误通知（4秒后自动关闭）
     */
    public static void showError(String message) {
        showToast(message, NotificationType.ERROR, 4);
    }

    /**
     * 显示可关闭的通知（带关闭按钮）
     */
    public static void showCloseable(String message, NotificationType type, int seconds) {
        showToast(message, type, seconds, defaultPosition, true);
    }

    /**
     * 显示长文本通知（支持展开/收起）
     */
    public static void showLongMessage(String message, NotificationType type) {
        showToast(message, type, 5, defaultPosition, true);
    }

    /**
     * 显示 Toast 通知
     */
    public static void showToast(String message, NotificationType type, int seconds) {
        showToast(message, type, seconds, defaultPosition, false);
    }

    /**
     * 显示 Toast 通知（指定位置）
     */
    public static void showToast(String message, NotificationType type, int seconds, NotificationPosition position) {
        showToast(message, type, seconds, position, false);
    }

    /**
     * 显示 Toast 通知（完整参数）
     */
    private static void showToast(String message, NotificationType type, int seconds, NotificationPosition position, boolean closeable) {
        SwingUtilities.invokeLater(() -> {
            Window mainFrame = getMainFrame();
            ToastWindow toast = new ToastWindow(mainFrame, message, type, seconds, position, closeable);
            synchronized (activeToasts) {
                activeToasts.add(toast);
                updateToastPositions();
            }
            toast.setVisible(true);
        });
    }

    /**
     * 更新所有 Toast 的位置（堆叠显示）
     */
    private static void updateToastPositions() {
        synchronized (activeToasts) {
            int offset = 0;
            for (ToastWindow toast : activeToasts) {
                toast.updateStackOffset(offset);
                offset += toast.getHeight() + 10; // 10px 间距
            }
        }
    }

    /**
     * 移除已关闭的 Toast
     */
    private static void removeToast(ToastWindow toast) {
        synchronized (activeToasts) {
            activeToasts.remove(toast);
            updateToastPositions();
        }
    }

    /**
     * Toast 窗口类
     */
    private static class ToastWindow extends JWindow {
        private static final int PADDING = 16;
        private static final int MIN_WIDTH = 300;
        private static final int MAX_WIDTH = 500;
        private static final int CORNER_RADIUS = 10;
        private static final int COLLAPSED_MAX_LINES = 3;
        private static final int SHADOW_SIZE = 8;

        private final Window parentWindow;
        private final NotificationType type;
        private final NotificationPosition position;
        private final String fullMessage;

        private float opacity = 0.0f;
        private int stackOffset = 0;
        private boolean isHovered = false;
        private boolean isExpanded = false;
        private Timer autoCloseTimer;
        private Timer progressTimer;
        private Timer fadeInTimer;
        private JLabel messageLabel;
        private JPanel mainPanel;
        private JProgressBar progressBar;
        private long pausedTime = 0;
        private long startTime = 0;
        private int totalDuration = 0;

        public ToastWindow(Window parentWindow, String message, NotificationType type, int seconds,
                           NotificationPosition position, boolean closeable) {
            super(parentWindow);
            this.parentWindow = parentWindow;
            this.type = type;
            this.position = position;
            this.fullMessage = message;

            setAlwaysOnTop(true);
            setFocusableWindowState(false);

            // 创建内容面板
            JPanel contentPanel = createContentPanel(message, seconds, closeable);
            setContentPane(contentPanel);

            // 设置窗口形状和透明度
            setBackground(new Color(0, 0, 0, 0));
            pack();

            // 设置初始位置（从右侧滑入）
            Point finalPosition = calculatePosition();
            Point startPosition = new Point(finalPosition.x + 300, finalPosition.y);
            setLocation(startPosition);

            // 滑入 + 渐入动画
            startSlideInAnimation(startPosition, finalPosition, seconds);
        }

        private JPanel createContentPanel(String message, int seconds, boolean showCloseButton) {
            mainPanel = new JPanel(new BorderLayout(0, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    // 绘制多层阴影（优化：使用更少的层数，更好的渐变效果）
                    int shadowLayers = 5;
                    for (int i = 0; i < shadowLayers; i++) {
                        float ratio = (float) i / shadowLayers;
                        float alpha = opacity * (1 - ratio) * 0.08f;
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        g2.setColor(Color.BLACK);
                        float offset = SHADOW_SIZE * ratio;
                        g2.fill(new RoundRectangle2D.Float(
                                offset, offset,
                                getWidth() - offset * 2,
                                getHeight() - offset * 2,
                                CORNER_RADIUS + offset,
                                CORNER_RADIUS + offset));
                    }

                    // 绘制圆角背景
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                    boolean isDark = UIManager.getBoolean("laf.dark");
                    Color bgColor = isDark ? new Color(45, 45, 48) : new Color(255, 255, 255);
                    g2.setColor(bgColor);
                    g2.fill(new RoundRectangle2D.Float(
                            SHADOW_SIZE, SHADOW_SIZE,
                            getWidth() - (float) SHADOW_SIZE * 2,
                            getHeight() - (float) SHADOW_SIZE * 2,
                            CORNER_RADIUS, CORNER_RADIUS));

                    // 绘制左侧彩色指示条
                    g2.setColor(type.getColor());
                    g2.fill(new RoundRectangle2D.Float(
                            SHADOW_SIZE, SHADOW_SIZE,
                            4,
                            getHeight() - (float) SHADOW_SIZE * 2,
                            CORNER_RADIUS, CORNER_RADIUS));

                    // 悬停时绘制边框高亮
                    if (isHovered) {
                        g2.setStroke(new BasicStroke(2f));
                        Color borderColor = new Color(
                                type.getColor().getRed(),
                                type.getColor().getGreen(),
                                type.getColor().getBlue(),
                                120);
                        g2.setColor(borderColor);
                        g2.draw(new RoundRectangle2D.Float(
                                (float) SHADOW_SIZE + 1, (float) SHADOW_SIZE + 1,
                                getWidth() - (float) SHADOW_SIZE * 2 - 2,
                                getHeight() - (float) SHADOW_SIZE * 2 - 2,
                                CORNER_RADIUS, CORNER_RADIUS));
                    }

                    g2.dispose();
                }
            };
            mainPanel.setOpaque(false);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(
                    SHADOW_SIZE + PADDING,
                    SHADOW_SIZE + PADDING + 5,
                    SHADOW_SIZE + PADDING,
                    SHADOW_SIZE + PADDING));

            // 顶部：图标 + 消息 + 关闭按钮
            JPanel topPanel = new JPanel(new BorderLayout(10, 0));
            topPanel.setOpaque(false);

            // 图标
            JLabel iconLabel = new JLabel(type.getIcon());
            iconLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 20));
            iconLabel.setForeground(type.getColor());
            iconLabel.setVerticalAlignment(SwingConstants.TOP);
            iconLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

            // 消息内容
            messageLabel = createMessageLabel(message);

            // 关闭按钮
            if (showCloseButton) {
                JPanel centerPanel = new JPanel(new BorderLayout());
                centerPanel.setOpaque(false);
                centerPanel.add(messageLabel, BorderLayout.CENTER);

                JButton closeButton = createCloseButton();
                topPanel.add(iconLabel, BorderLayout.WEST);
                topPanel.add(centerPanel, BorderLayout.CENTER);
                topPanel.add(closeButton, BorderLayout.EAST);
            } else {
                topPanel.add(iconLabel, BorderLayout.WEST);
                topPanel.add(messageLabel, BorderLayout.CENTER);
            }

            mainPanel.add(topPanel, BorderLayout.CENTER);

            // 底部：进度条（可配置）
            if (seconds > 0 && showProgressBar) {
                progressBar = createProgressBar();
                JPanel progressPanel = new JPanel(new BorderLayout());
                progressPanel.setOpaque(false);
                progressPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
                progressPanel.add(progressBar, BorderLayout.CENTER);
                mainPanel.add(progressPanel, BorderLayout.SOUTH);
            }

            // 添加交互事件
            addInteractionListeners(mainPanel);

            // 计算尺寸
            Dimension prefSize = mainPanel.getPreferredSize();
            int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, prefSize.width + SHADOW_SIZE * 2));
            int height = prefSize.height + SHADOW_SIZE * 2;
            mainPanel.setPreferredSize(new Dimension(width, height));

            return mainPanel;
        }

        private JLabel createMessageLabel(String message) {
            String html = formatMessageAsHtml(message, false);
            JLabel label = new JLabel(html);
            Font baseFont = UIManager.getFont("Label.font");
            if (baseFont != null) {
                label.setFont(baseFont.deriveFont(13f));
            }
            label.setVerticalAlignment(SwingConstants.TOP);

            // 设置文本颜色
            boolean isDark = UIManager.getBoolean("laf.dark");
            label.setForeground(isDark ? new Color(230, 230, 230) : new Color(50, 50, 50));

            return label;
        }

        private String formatMessageAsHtml(String message, boolean expanded) {
            if (message == null || message.isEmpty()) {
                return "";
            }

            // 转义 HTML
            String escaped = message.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");

            // 处理换行
            String[] lines = escaped.split("\n");
            StringBuilder html = new StringBuilder("<html><body style='width: ");
            html.append(MAX_WIDTH - 120).append("px; line-height: 1.4;'>");

            if (!expanded && lines.length > COLLAPSED_MAX_LINES) {
                // 折叠模式：只显示前几行
                for (int i = 0; i < COLLAPSED_MAX_LINES; i++) {
                    html.append(lines[i]);
                    if (i < COLLAPSED_MAX_LINES - 1) {
                        html.append("<br/>");
                    }
                }
                html.append("... <span style='color: ").append(toHex(type.getColor()))
                        .append("; font-weight: bold; cursor: pointer;'>[展开]</span>");
            } else {
                // 展开模式：显示全部
                for (int i = 0; i < lines.length; i++) {
                    html.append(lines[i]);
                    if (i < lines.length - 1) {
                        html.append("<br/>");
                    }
                }
                if (lines.length > COLLAPSED_MAX_LINES) {
                    html.append(" <span style='color: ").append(toHex(type.getColor()))
                            .append("; font-weight: bold; cursor: pointer;'>[收起]</span>");
                }
            }

            html.append("</body></html>");
            return html.toString();
        }

        private String toHex(Color color) {
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }

        private JButton createCloseButton() {
            JButton button = new JButton("✕");
            button.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 16));
            button.setForeground(ModernColors.TEXT_SECONDARY);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setPreferredSize(new Dimension(24, 24));

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setForeground(ModernColors.ERROR);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setForeground(ModernColors.TEXT_SECONDARY);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    closeImmediately();
                }
            });

            return button;
        }

        private JProgressBar createProgressBar() {
            JProgressBar bar = new JProgressBar(0, 100) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int width = getWidth();
                    int height = getHeight();
                    int progressWidth = (int) (width * getValue() / 100.0);

                    // 绘制背景（浅色）
                    g2.setColor(new Color(
                            type.getColor().getRed(),
                            type.getColor().getGreen(),
                            type.getColor().getBlue(),
                            30));
                    g2.fillRoundRect(0, 0, width, height, height, height);

                    // 绘制进度（使用渐变）
                    if (progressWidth > 0) {
                        GradientPaint gradient = new GradientPaint(
                                0, 0, type.getColor(),
                                progressWidth, 0, new Color(
                                type.getColor().getRed(),
                                type.getColor().getGreen(),
                                type.getColor().getBlue(),
                                180)
                        );
                        g2.setPaint(gradient);
                        g2.fillRoundRect(0, 0, progressWidth, height, height, height);
                    }

                    g2.dispose();
                }
            };

            bar.setValue(100);
            bar.setPreferredSize(new Dimension(0, 4)); // 从2px增加到4px，更明显
            bar.setBorderPainted(false);
            bar.setOpaque(false);
            bar.setStringPainted(false);

            return bar;
        }

        private void addInteractionListeners(JPanel panel) {
            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    pauseAutoClose();
                    panel.repaint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    resumeAutoClose();
                    panel.repaint();
                    setCursor(Cursor.getDefaultCursor());
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        // 单击：展开/收起或复制
                        if (fullMessage.split("\n").length > COLLAPSED_MAX_LINES) {
                            toggleExpand();
                        } else {
                            copyToClipboard(fullMessage);
                            showCopyFeedback();
                        }
                    } else if (e.getClickCount() == 2) {
                        // 双击：复制内容
                        copyToClipboard(fullMessage);
                        showCopyFeedback();
                    }
                }
            };

            panel.addMouseListener(adapter);
            messageLabel.addMouseListener(adapter);
        }

        private void toggleExpand() {
            isExpanded = !isExpanded;
            messageLabel.setText(formatMessageAsHtml(fullMessage, isExpanded));
            pack();
            setLocation(calculatePosition());
            updateToastPositions();
        }

        private void copyToClipboard(String text) {
            try {
                StringSelection selection = new StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            } catch (Exception e) {
                // 忽略剪贴板错误
            }
        }

        private void showCopyFeedback() {
            // 简单的视觉反馈
            Color originalColor = messageLabel.getForeground();
            messageLabel.setForeground(type.getColor());
            Timer timer = new Timer(200, e -> messageLabel.setForeground(originalColor));
            timer.setRepeats(false);
            timer.start();
        }

        private void startSlideInAnimation(Point start, Point end, int seconds) {
            final int ANIMATION_DURATION = 400; // ms - 增加动画时长使其更流畅
            final int ANIMATION_STEPS = 20;
            final int DELAY = ANIMATION_DURATION / ANIMATION_STEPS;

            fadeInTimer = new Timer(DELAY, null);
            final int[] step = {0};

            fadeInTimer.addActionListener(e -> {
                step[0]++;
                float progress = (float) step[0] / ANIMATION_STEPS;

                // 使用更平滑的缓动函数（ease-out-cubic）
                float eased = 1 - (float) Math.pow(1 - progress, 3);

                // 更新位置
                int x = (int) (start.x + (end.x - start.x) * eased);
                int y = (int) (start.y + (end.y - start.y) * eased);
                setLocation(x, y);

                // 更新透明度（同样使用缓动）
                opacity = eased * 0.98f;
                mainPanel.repaint();

                if (step[0] >= ANIMATION_STEPS) {
                    fadeInTimer.stop();
                    opacity = 0.98f;
                    // 开始自动关闭倒计时
                    if (seconds > 0) {
                        startAutoCloseTimer(seconds);
                    }
                }
            });
            fadeInTimer.start();
        }

        private void startAutoCloseTimer(int seconds) {
            this.totalDuration = seconds * 1000;
            this.startTime = System.currentTimeMillis();

            // 进度条动画
            if (progressBar != null) {
                progressTimer = new Timer(50, e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    int progress = (int) (100 - (elapsed * 100.0 / totalDuration));
                    progressBar.setValue(Math.max(0, progress));

                    if (progress <= 0) {
                        progressTimer.stop();
                    }
                });
                progressTimer.start();
            }

            // 自动关闭定时器
            autoCloseTimer = new Timer(totalDuration, e -> fadeOut());
            autoCloseTimer.setRepeats(false);
            autoCloseTimer.start();
        }

        private void pauseAutoClose() {
            // 记录暂停时的时间点
            pausedTime = System.currentTimeMillis();

            if (autoCloseTimer != null && autoCloseTimer.isRunning()) {
                autoCloseTimer.stop();
            }

            if (progressTimer != null && progressTimer.isRunning()) {
                progressTimer.stop();
            }
        }

        private void resumeAutoClose() {
            if (pausedTime > 0) {
                // 计算暂停了多长时间，并调整 startTime
                long pauseDuration = System.currentTimeMillis() - pausedTime;
                startTime += pauseDuration;
                pausedTime = 0;

                // 计算剩余时间
                long elapsed = System.currentTimeMillis() - startTime;
                int remaining = (int) (totalDuration - elapsed);

                if (remaining > 0) {
                    // 重启进度条动画
                    if (progressTimer != null && !progressTimer.isRunning()) {
                        progressTimer.start();
                    }

                    // 重启自动关闭定时器（使用剩余时间）
                    if (autoCloseTimer != null) {
                        autoCloseTimer.stop();
                        autoCloseTimer = new Timer(remaining, e -> fadeOut());
                        autoCloseTimer.setRepeats(false);
                        autoCloseTimer.start();
                    }
                } else {
                    // 时间已到，直接关闭
                    fadeOut();
                }
            }
        }

        private void closeImmediately() {
            if (autoCloseTimer != null) {
                autoCloseTimer.stop();
            }
            if (progressTimer != null) {
                progressTimer.stop();
            }
            if (fadeInTimer != null) {
                fadeInTimer.stop();
            }
            fadeOut();
        }

        private Point calculatePosition() {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension windowSize = getSize();

            Rectangle parentBounds = parentWindow != null ? parentWindow.getBounds() :
                    new Rectangle(0, 0, screenSize.width, screenSize.height);

            int x = 0;
            int y = 0;
            int margin = 20; // 增加边距，更优雅

            switch (position) {
                case TOP_RIGHT:
                    x = parentBounds.x + parentBounds.width - windowSize.width - margin;
                    y = parentBounds.y + margin + stackOffset;
                    break;
                case TOP_CENTER:
                    x = parentBounds.x + (parentBounds.width - windowSize.width) / 2;
                    y = parentBounds.y + margin + stackOffset;
                    break;
                case TOP_LEFT:
                    x = parentBounds.x + margin;
                    y = parentBounds.y + margin + stackOffset;
                    break;
                case BOTTOM_RIGHT:
                    x = parentBounds.x + parentBounds.width - windowSize.width - margin;
                    y = parentBounds.y + parentBounds.height - windowSize.height - margin - stackOffset;
                    break;
                case BOTTOM_CENTER:
                    x = parentBounds.x + (parentBounds.width - windowSize.width) / 2;
                    y = parentBounds.y + parentBounds.height - windowSize.height - margin - stackOffset;
                    break;
                case BOTTOM_LEFT:
                    x = parentBounds.x + margin;
                    y = parentBounds.y + parentBounds.height - windowSize.height - margin - stackOffset;
                    break;
                case CENTER:
                    x = parentBounds.x + (parentBounds.width - windowSize.width) / 2;
                    y = parentBounds.y + (parentBounds.height - windowSize.height) / 2 + stackOffset;
                    break;
            }

            return new Point(x, y);
        }

        public void updateStackOffset(int offset) {
            this.stackOffset = offset;
            setLocation(calculatePosition());
        }

        private void fadeOut() {
            Timer fadeOutTimer = new Timer(25, null);
            fadeOutTimer.addActionListener(e -> {
                opacity -= 0.12f;
                if (opacity <= 0) {
                    opacity = 0;
                    fadeOutTimer.stop();
                    dispose();
                    removeToast(this);
                }
                if (mainPanel != null) {
                    mainPanel.repaint();
                }
            });
            fadeOutTimer.start();
        }
    }
}

