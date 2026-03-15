package com.laker.postman.util;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.NotificationPosition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ IDEA 风格的 Balloon 通知工具类
 */
@UtilityClass
public class NotificationUtil {

    private static Window getMainFrame() {
        try {
            return SingletonFactory.getInstance(MainFrame.class);
        } catch (Exception e) {
            return JOptionPane.getRootFrame();
        }
    }

    // ==================== 通知类型 ====================

    @Getter
    @RequiredArgsConstructor
    public enum NotificationType {
        SUCCESS(ModernColors.SUCCESS, "✓", MessageKeys.NOTIFICATION_TYPE_SUCCESS),
        INFO(ModernColors.INFO, "i", MessageKeys.NOTIFICATION_TYPE_INFO),
        WARNING(ModernColors.WARNING, "!", MessageKeys.NOTIFICATION_TYPE_WARNING),
        ERROR(ModernColors.ERROR, "✕", MessageKeys.NOTIFICATION_TYPE_ERROR);

        private final Color color;
        private final String icon;
        private final String titleKey;

        public String getDefaultTitle() {
            return I18nUtil.getMessage(titleKey);
        }
    }

    // ==================== 全局配置 ====================

    @Setter
    private static NotificationPosition defaultPosition = NotificationPosition.BOTTOM_RIGHT;

    private static final int MAX_ACTIVE_TOASTS = 5;

    /**
     * Sidebar 底部工具栏高度，通知需要在其之上显示，避免遮挡。
     * consoleContainer 实际高度约 32px，再加一点间隙。
     */
    private static final int SIDEBAR_BOTTOM_BAR_HEIGHT = 34;

    private static final List<ToastWindow> activeToasts = new ArrayList<>();

    // ==================== 公开 API ====================

    public static void showSuccess(String message) {
        showToast(message, NotificationType.SUCCESS, 2);
    }

    public static void showInfo(String message) {
        showToast(message, NotificationType.INFO, 2);
    }

    public static void showWarning(String message) {
        showToast(message, NotificationType.WARNING, 3);
    }

    public static void showError(String message) {
        showToast(message, NotificationType.ERROR, 3);
    }

    public static void showCloseable(String message, NotificationType type, int seconds) {
        showToast(message, type, seconds, defaultPosition, null);
    }

    public static void showLongMessage(String message, NotificationType type) {
        showToast(message, type, 5, defaultPosition, null);
    }

    public static void showToast(String message, NotificationType type, int seconds) {
        showToast(message, type, seconds, defaultPosition, null);
    }

    public static void showToast(String message, NotificationType type, int seconds, NotificationPosition position) {
        showToast(message, type, seconds, position, null);
    }

    public static void showToast(String message, NotificationType type, int seconds, String title) {
        showToast(message, type, seconds, defaultPosition, title);
    }

    // ==================== 内部实现 ====================

    private static void showToast(String message, NotificationType type, int seconds,
                                  NotificationPosition position, String title) {
        SwingUtilities.invokeLater(() -> {
            Window mainFrame = getMainFrame();
            ToastWindow toast = new ToastWindow(mainFrame, message, title, type, seconds, position);
            synchronized (activeToasts) {
                while (activeToasts.size() >= MAX_ACTIVE_TOASTS) {
                    activeToasts.get(0).closeQuietly();
                }
                activeToasts.add(toast);
                updateToastPositions();
            }
            toast.startShow();
        });
    }

    private static void updateToastPositions() {
        synchronized (activeToasts) {
            int offset = 0;
            for (ToastWindow toast : activeToasts) {
                toast.updateStackOffset(offset);
                offset += toast.getHeight() + 6;
            }
        }
    }

    private static void removeToast(ToastWindow toast) {
        synchronized (activeToasts) {
            activeToasts.remove(toast);
            updateToastPositions();
        }
    }

    // ==================== ToastWindow ====================

    private static class ToastWindow extends JWindow {

        // ---- 尺寸 ----
        private static final int MIN_WIDTH = 300;
        private static final int MAX_WIDTH = 440;
        private static final int CORNER_RADIUS = 8;
        private static final int HEADER_H = 30;
        private static final int H_PAD = 14;
        private static final int V_PAD = 10;
        private static final int COLLAPSED_MAX_LINES = 4;
        private static final int SLIDE_STEPS = 14;
        private static final int SLIDE_INTERVAL = 14;  // ~70 fps
        private static final int FADE_STEPS = 10;
        private static final int FADE_INTERVAL = 18;

        // ---- 状态 ----
        private final Window parentWindow;
        private final NotificationType type;
        private final NotificationPosition position;
        private final String fullMessage;

        private int stackOffset = 0;
        private boolean isHovered = false;
        private boolean isExpanded = false;
        private int slideStep = 0;
        private Point targetPos;

        private Timer autoCloseTimer;
        private Timer slideTimer;
        private Timer fadeTimer;
        private JTextArea bodyLabel;
        private JPanel rootPanel;
        private JButton closeButton;

        private long pausedTime = 0;
        private long startTime = 0;
        private int totalDuration = 0;

        public ToastWindow(Window parentWindow, String message, String customTitle,
                           NotificationType type, int seconds, NotificationPosition position) {
            super(parentWindow);
            this.parentWindow = parentWindow;
            this.type = type;
            this.position = position;
            this.fullMessage = message;

            setAlwaysOnTop(true);
            setFocusableWindowState(false);
            setBackground(new Color(0, 0, 0, 0));
            getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);

            String title = (customTitle != null && !customTitle.isBlank())
                    ? customTitle : type.getDefaultTitle();
            rootPanel = buildRootPanel(message, title, seconds);
            setContentPane(rootPanel);
            // 先固定宽度并 pack()，使 JTextArea 按实际宽度折行后再计算高度
            setSize(MAX_WIDTH, 1);
            // 让 JTextArea 以分配到的宽度计算折行后的 preferred height
            int bodyW = MAX_WIDTH - H_PAD * 2;
            bodyLabel.setSize(bodyW, 1);
            int finalH = rootPanel.getPreferredSize().height;
            setSize(MAX_WIDTH, finalH);
            targetPos = calculatePosition(stackOffset);
        }

        // ==================== 动画入场 ====================

        public void startShow() {
            Point startPos = getSlideStartPosition();
            setLocation(startPos);
            setVisible(true);

            slideStep = 0;
            slideTimer = new Timer(SLIDE_INTERVAL, e -> {
                slideStep++;
                double t = (double) slideStep / SLIDE_STEPS;
                double eased = 1 - Math.pow(1 - t, 3);
                int x = startPos.x + (int) ((targetPos.x - startPos.x) * eased);
                int y = startPos.y + (int) ((targetPos.y - startPos.y) * eased);
                setLocation(x, y);
                if (slideStep >= SLIDE_STEPS) {
                    ((Timer) e.getSource()).stop();
                    setLocation(targetPos);
                    if (totalDuration > 0) startAutoCloseTimer();
                }
            });
            slideTimer.start();
        }

        private Point getSlideStartPosition() {
            Point p = new Point(targetPos);
            switch (position) {
                case BOTTOM_RIGHT:
                case BOTTOM_CENTER:
                case BOTTOM_LEFT:
                    p.y += getHeight() + 16;
                    break;
                default:
                    p.y -= getHeight() + 16;
                    break;
            }
            return p;
        }

        // ==================== UI 构建 ====================

        private JPanel buildRootPanel(String message, String title, int seconds) {
            JPanel wrapper = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = setupG2(g);
                    int w = getWidth(), h = getHeight();
                    boolean dark = FlatLaf.isLafDark();

                    // 扁平圆角背景
                    Color bg = dark ? new Color(47, 49, 52) : new Color(252, 252, 253);
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS));

                    // 1px 扁平边框
                    Color border = dark ? new Color(72, 74, 78) : new Color(205, 208, 214);
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, CORNER_RADIUS, CORNER_RADIUS));
                    g2.dispose();
                }
            };
            wrapper.setOpaque(false);
            wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            getRootPane().setOpaque(false);
            getRootPane().setBackground(new Color(0, 0, 0, 0));
            getLayeredPane().setOpaque(false);

            JPanel card = new JPanel(new BorderLayout());
            card.setOpaque(false);

            JPanel header = buildHeaderPanel(title);
            card.add(header, BorderLayout.NORTH);

            JPanel body = buildBodyPanel(message);
            card.add(body, BorderLayout.CENTER);

            wrapper.add(card, BorderLayout.CENTER);

            for (JPanel p : new JPanel[]{wrapper, card, header, body}) {
                addHoverListener(p);
            }

            totalDuration = seconds * 1000;
            return wrapper;
        }

        private JPanel buildHeaderPanel(String title) {
            JPanel header = new JPanel(new BorderLayout(6, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = setupG2(g);
                    int w = getWidth(), h = getHeight();
                    boolean dark = FlatLaf.isLafDark();

                    Color baseColor = type.getColor();
                    Color headerBg = dark ? blendWithDark(baseColor, 0.16f) : blendWithLight(baseColor, 0.10f);
                    g2.setColor(headerBg);
                    // 只裁剪上方两角
                    g2.setClip(new RoundRectangle2D.Float(0, 0, w, h + CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS));
                    g2.fillRect(0, 0, w, h);
                    g2.setClip(null);

                    // 分隔线
                    Color divider = dark ? new Color(65, 67, 71) : new Color(208, 211, 216);
                    g2.setColor(divider);
                    g2.drawLine(0, h - 1, w, h - 1);
                    g2.dispose();
                }
            };
            header.setOpaque(false);
            header.setPreferredSize(new Dimension(0, HEADER_H));
            header.setBorder(BorderFactory.createEmptyBorder(0, H_PAD, 0, 4));

            // 彩色圆点图标 badge
            JLabel iconLabel = new JLabel(type.getIcon()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = setupG2(g);
                    int sz = Math.min(getWidth(), getHeight()) - 2;
                    int ox = (getWidth() - sz) / 2;
                    int oy = (getHeight() - sz) / 2;
                    g2.setColor(type.getColor());
                    g2.fillOval(ox, oy, sz, sz);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            iconLabel.setForeground(Color.WHITE);
            iconLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setVerticalAlignment(SwingConstants.CENTER);
            iconLabel.setPreferredSize(new Dimension(18, 18));

            // 标题
            JLabel titleLabel = new JLabel(title);
            boolean dark = FlatLaf.isLafDark();
            titleLabel.setForeground(dark ? brighten(type.getColor(), 0.3f) : darken(type.getColor(), 0.2f));
            titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));

            // 关闭按钮 ×（默认透明隐藏，hover 时淡入）
            closeButton = buildCloseButton();
            closeButton.setOpaque(false);

            header.add(iconLabel, BorderLayout.WEST);
            header.add(titleLabel, BorderLayout.CENTER);
            header.add(closeButton, BorderLayout.EAST);

            return header;
        }


        private JPanel buildBodyPanel(String message) {
            JPanel body = new JPanel(new BorderLayout());
            body.setOpaque(false);
            body.setBorder(BorderFactory.createEmptyBorder(V_PAD, H_PAD, V_PAD, H_PAD));

            bodyLabel = new JTextArea(getDisplayText(message, false));
            bodyLabel.setLineWrap(true);
            bodyLabel.setWrapStyleWord(true);
            bodyLabel.setEditable(false);
            bodyLabel.setFocusable(false);
            bodyLabel.setOpaque(false);
            bodyLabel.setBorder(null);
            boolean dark = FlatLaf.isLafDark();
            bodyLabel.setForeground(dark ? new Color(210, 213, 216) : new Color(44, 46, 50));
            Font lf = UIManager.getFont("Label.font");
            if (lf != null) bodyLabel.setFont(lf.deriveFont(Font.PLAIN, lf.getSize2D()));

            body.add(bodyLabel, BorderLayout.CENTER);

            MouseAdapter click = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        boolean longMsg = fullMessage.split("\n", -1).length > COLLAPSED_MAX_LINES
                                || fullMessage.length() > 120;
                        if (longMsg) toggleExpand();
                        else {
                            copyToClipboard(fullMessage);
                            flashFeedback();
                        }
                    } else if (e.getClickCount() == 2) {
                        copyToClipboard(fullMessage);
                        flashFeedback();
                    }
                }
            };
            body.addMouseListener(click);
            bodyLabel.addMouseListener(click);
            return body;
        }

        private JButton buildCloseButton() {
            JButton btn = new JButton("×");
            btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 15f));
            btn.setForeground(new Color(150, 153, 158, 0));  // 初始完全透明
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(26, 26));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setForeground(type.getColor());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // 还在 window 内就保持 hint 色
                    btn.setForeground(ModernColors.getTextHint());
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    startFadeOut();
                }
            });
            return btn;
        }

        /**
         * 统一的 hover 监听，附加到任意 JPanel
         */
        private void addHoverListener(JPanel panel) {
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    onHoverEnter();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ToastWindow.this);
                    if (!new Rectangle(getSize()).contains(pt)) onHoverExit();
                }
            });
        }

        private void onHoverEnter() {
            if (isHovered) return;
            isHovered = true;
            pauseAutoClose();
            closeButton.setForeground(ModernColors.getTextHint());
        }

        private void onHoverExit() {
            if (!isHovered) return;
            isHovered = false;
            resumeAutoClose();
            closeButton.setForeground(new Color(150, 153, 158, 0));
        }


        // ==================== 动画 & 定时器 ====================

        private void startAutoCloseTimer() {
            startTime = System.currentTimeMillis();
            autoCloseTimer = new Timer(totalDuration, e -> startFadeOut());
            autoCloseTimer.setRepeats(false);
            autoCloseTimer.start();
        }

        private void pauseAutoClose() {
            pausedTime = System.currentTimeMillis();
            if (autoCloseTimer != null) autoCloseTimer.stop();
        }

        private void resumeAutoClose() {
            if (pausedTime > 0) {
                long paused = System.currentTimeMillis() - pausedTime;
                startTime += paused;
                pausedTime = 0;
                long remaining = totalDuration - (System.currentTimeMillis() - startTime);
                if (remaining > 0) {
                    autoCloseTimer = new Timer((int) remaining, e -> startFadeOut());
                    autoCloseTimer.setRepeats(false);
                    autoCloseTimer.start();
                } else {
                    startFadeOut();
                }
            }
        }

        /**
         * fade-out 动画后关闭
         */
        private void startFadeOut() {
            stopTimers();
            final int[] step = {0};
            fadeTimer = new Timer(FADE_INTERVAL, e -> {
                step[0]++;
                float alpha = Math.max(0f, 1f - (float) step[0] / FADE_STEPS);
                setOpacity(alpha);
                if (step[0] >= FADE_STEPS) {
                    ((Timer) e.getSource()).stop();
                    dispose();
                    removeToast(this);
                }
            });
            fadeTimer.start();
        }

        void closeQuietly() {
            stopTimers();
            dispose();
            removeToast(this);
        }

        private void stopTimers() {
            if (autoCloseTimer != null) autoCloseTimer.stop();
            if (slideTimer != null) slideTimer.stop();
        }

        // ==================== 展开/收起 ====================

        private void toggleExpand() {
            isExpanded = !isExpanded;
            bodyLabel.setText(getDisplayText(fullMessage, isExpanded));
            int bodyW = MAX_WIDTH - H_PAD * 2;
            bodyLabel.setSize(bodyW, 1);
            int finalH = rootPanel.getPreferredSize().height;
            setSize(MAX_WIDTH, finalH);
            targetPos = calculatePosition(stackOffset);
            setLocation(targetPos);
            updateToastPositions();
        }

        // ==================== 位置计算 ====================

        private Point calculatePosition(int offset) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension winSize = getSize();
            Rectangle pb = (parentWindow != null)
                    ? parentWindow.getBounds()
                    : new Rectangle(0, 0, screenSize.width, screenSize.height);

            final int margin = 14;
            int x, y;
            switch (position) {
                case TOP_RIGHT:
                    x = pb.x + pb.width - winSize.width - margin;
                    y = pb.y + margin + offset;
                    break;
                case TOP_CENTER:
                    x = pb.x + (pb.width - winSize.width) / 2;
                    y = pb.y + margin + offset;
                    break;
                case TOP_LEFT:
                    x = pb.x + margin;
                    y = pb.y + margin + offset;
                    break;
                case BOTTOM_RIGHT:
                    x = pb.x + pb.width - winSize.width - margin;
                    // 加上 sidebar 底部栏高度，避免遮挡
                    y = pb.y + pb.height - winSize.height - margin - SIDEBAR_BOTTOM_BAR_HEIGHT - offset;
                    break;
                case BOTTOM_CENTER:
                    x = pb.x + (pb.width - winSize.width) / 2;
                    y = pb.y + pb.height - winSize.height - margin - SIDEBAR_BOTTOM_BAR_HEIGHT - offset;
                    break;
                case BOTTOM_LEFT:
                    x = pb.x + margin;
                    y = pb.y + pb.height - winSize.height - margin - SIDEBAR_BOTTOM_BAR_HEIGHT - offset;
                    break;
                case CENTER:
                    x = pb.x + (pb.width - winSize.width) / 2;
                    y = pb.y + (pb.height - winSize.height) / 2 + offset;
                    break;
                default:
                    x = pb.x + pb.width - winSize.width - margin;
                    y = pb.y + pb.height - winSize.height - margin - SIDEBAR_BOTTOM_BAR_HEIGHT - offset;
                    break;
            }
            return new Point(x, y);
        }

        void updateStackOffset(int offset) {
            stackOffset = offset;
            targetPos = calculatePosition(offset);
            setLocation(targetPos);
        }

        // ==================== 工具方法 ====================

        private Graphics2D setupG2(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            return g2;
        }

        private Color blendWithDark(Color c, float ratio) {
            return ModernColors.blendColors(new Color(47, 49, 52), c, ratio);
        }

        private Color blendWithLight(Color c, float ratio) {
            return ModernColors.blendColors(new Color(252, 252, 253), c, ratio);
        }

        private Color darken(Color c, float f) {
            return new Color(Math.max(0, (int) (c.getRed() * (1 - f))), Math.max(0, (int) (c.getGreen() * (1 - f))), Math.max(0, (int) (c.getBlue() * (1 - f))));
        }

        private Color brighten(Color c, float f) {
            return new Color(Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * f)), Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * f)), Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * f)));
        }


        /**
         * 根据展开状态返回显示文本（纯文本，供 JTextArea 使用）。
         * 折叠时只显示前 COLLAPSED_MAX_LINES 行，末尾加省略提示。
         */
        private String getDisplayText(String message, boolean expanded) {
            if (message == null || message.isBlank()) return "";
            String[] lines = message.split("\n", -1);
            boolean needFold = lines.length > COLLAPSED_MAX_LINES || message.length() > 120;
            if (!expanded && needFold) {
                int show = Math.min(lines.length, COLLAPSED_MAX_LINES);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < show; i++) {
                    sb.append(lines[i]);
                    if (i < show - 1) sb.append("\n");
                }
                sb.append("… ").append(I18nUtil.getMessage(MessageKeys.NOTIFICATION_EXPAND));
                return sb.toString();
            }
            return needFold ? message + " " + I18nUtil.getMessage(MessageKeys.NOTIFICATION_COLLAPSE) : message;
        }

        private void copyToClipboard(String text) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(text), null);
            } catch (Exception ignored) {
            }
        }

        private void flashFeedback() {
            Color orig = bodyLabel.getForeground();
            bodyLabel.setForeground(type.getColor());
            new Timer(200, e -> {
                bodyLabel.setForeground(orig);
                ((Timer) e.getSource()).stop();
            }).start();
        }
    }
}
