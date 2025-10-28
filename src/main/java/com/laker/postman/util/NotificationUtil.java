package com.laker.postman.util;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Toast 风格的自动关闭通知工具类
 * Toast-style auto-close notification utility
 *
 * @author laker
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
        SUCCESS("#4CAF50", "✓"),    // 绿色 - 成功
        INFO("#2196F3", "ℹ"),       // 蓝色 - 信息
        WARNING("#FF9800", "⚠"),    // 橙色 - 警告
        ERROR("#F44336", "✕");      // 红色 - 错误

        private final String colorHex;
        private final String icon;

        NotificationType(String colorHex, String icon) {
            this.colorHex = colorHex;
            this.icon = icon;
        }

        public Color getColor() {
            return Color.decode(colorHex);
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

    // 当前显示的通知列表（用于堆叠管理）
    private static final List<ToastWindow> activeToasts = new ArrayList<>();


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
     * 显示 Toast 通知
     */
    public static void showToast(String message, NotificationType type, int seconds) {
        showToast(message, type, seconds, defaultPosition);
    }

    /**
     * 显示 Toast 通知（指定位置）
     */
    public static void showToast(String message, NotificationType type, int seconds, NotificationPosition position) {
        SwingUtilities.invokeLater(() -> {
            Window mainFrame = getMainFrame();
            ToastWindow toast = new ToastWindow(mainFrame, message, type, seconds, position);
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
        private static final int MIN_WIDTH = 200;
        private static final int MAX_WIDTH = 400;
        private static final int CORNER_RADIUS = 8;

        private final Window parentWindow;
        private final NotificationType type;
        private final NotificationPosition position;
        private float opacity = 0.0f;
        private int stackOffset = 0;

        public ToastWindow(Window parentWindow, String message, NotificationType type, int seconds, NotificationPosition position) {
            super(parentWindow);
            this.parentWindow = parentWindow;
            this.type = type;
            this.position = position;

            setAlwaysOnTop(true);
            setFocusableWindowState(false);

            // 创建内容面板
            JPanel contentPanel = createContentPanel(message);
            setContentPane(contentPanel);

            // 设置窗口形状和透明度
            setBackground(new Color(0, 0, 0, 0));
            pack();

            // 设置初始位置
            setLocation(calculatePosition());

            // 渐入动画
            Timer fadeInTimer = new Timer(20, null);
            fadeInTimer.addActionListener(e -> {
                opacity += 0.1f;
                if (opacity >= 0.95f) {
                    opacity = 0.95f;
                    fadeInTimer.stop();

                    // 等待指定时间后开始渐出
                    Timer waitTimer = new Timer(seconds * 1000, evt -> fadeOut());
                    waitTimer.setRepeats(false);
                    waitTimer.start();
                }
                contentPanel.repaint();
            });
            fadeInTimer.start();
        }

        private JPanel createContentPanel(String message) {
            JPanel panel = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 绘制圆角背景
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

                    // 背景色（深色/浅色主题自适应）
                    boolean isDark = UIManager.getBoolean("laf.dark");
                    Color bgColor = isDark ? new Color(60, 63, 65) : new Color(245, 245, 245);
                    g2.setColor(bgColor);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS));

                    // 绘制左侧彩色条
                    g2.setColor(type.getColor());
                    g2.fill(new RoundRectangle2D.Float(0, 0, 4, getHeight(), CORNER_RADIUS, CORNER_RADIUS));

                    // 绘制阴影效果
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.3f));
                    g2.setColor(Color.BLACK);
                    g2.draw(new RoundRectangle2D.Float(0, 0, (float) getWidth() - 1, (float) getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS));

                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING + 4, PADDING, PADDING));

            // 图标标签
            JLabel iconLabel = new JLabel(type.getIcon());
            iconLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 20));
            iconLabel.setForeground(type.getColor());
            iconLabel.setVerticalAlignment(SwingConstants.TOP); // 图标顶部对齐

            // 转义 HTML 特殊字符
            String escapedMessage = escapeHtml(message);

            // 限制消息长度，超长显示省略号
            int maxLength = 150; // 最多显示150个字符
            if (escapedMessage.length() > maxLength) {
                escapedMessage = escapedMessage.substring(0, maxLength) + "...";
            }

            // 消息标签 - 单行显示，超长截断
            JLabel messageLabel = new JLabel(escapedMessage);
            messageLabel.setFont(UIManager.getFont("Label.font"));

            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(messageLabel, BorderLayout.CENTER);

            // 计算合适的尺寸
            Dimension prefSize = panel.getPreferredSize();
            int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, prefSize.width));
            int height = Math.min(80, prefSize.height); // 限制最大高度为 80px（约2-3行）
            panel.setPreferredSize(new Dimension(width, height));

            return panel;
        }

        /**
         * 转义 HTML 特殊字符
         */
        private static String escapeHtml(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("\n", "<br/>");
        }

        private Point calculatePosition() {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension windowSize = getSize();

            Rectangle parentBounds = parentWindow != null ? parentWindow.getBounds() :
                    new Rectangle(0, 0, screenSize.width, screenSize.height);

            int x = 0, y = 0;
            int margin = 5; // 距离右边/下边缘的距离

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
            Timer fadeOutTimer = new Timer(20, null);
            fadeOutTimer.addActionListener(e -> {
                opacity -= 0.1f;
                if (opacity <= 0) {
                    opacity = 0;
                    fadeOutTimer.stop();
                    dispose();
                    removeToast(this);
                }
                getContentPane().repaint();
            });
            fadeOutTimer.start();
        }
    }
}

