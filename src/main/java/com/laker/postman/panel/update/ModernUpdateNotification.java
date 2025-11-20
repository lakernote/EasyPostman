package com.laker.postman.panel.update;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * 现代化更新通知 - 简洁优雅的右下角提示
 * 特性：
 * - 从右下角滑入/滑出动画
 * - 现代化配色和圆角设计
 * - 平滑的缓动效果
 * - 自动关闭（5秒）
 */
public class ModernUpdateNotification {

    private final JWindow window;
    private final Timer autoCloseTimer;
    private final Timer animationTimer;
    private int currentX;
    private int currentY;
    private int targetX;
    private int targetY;
    private int screenWidth;
    private int screenHeight;
    private boolean isSlideIn = true;

    // 动画参数
    private static final int ANIMATION_FPS = 60;
    private static final int ANIMATION_DELAY = 1000 / ANIMATION_FPS; // ~16ms
    private static final double EASING_FACTOR = 0.15; // 缓动系数，越小越平滑

    public ModernUpdateNotification(JFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        window = new JWindow(parent);
        window.setAlwaysOnTop(true);

        JPanel panel = createNotificationPanel(updateInfo, onViewDetails);
        window.setContentPane(panel);
        window.pack();

        // 获取屏幕高度
        screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        // 初始位置设在屏幕外（下方）
        positionWindowOffScreen(parent);

        // 自动关闭定时器（5秒）
        autoCloseTimer = new Timer(5000, e -> slideOut());
        autoCloseTimer.setRepeats(false);

        // 统一的动画定时器（用于滑入和滑出）
        animationTimer = new Timer(ANIMATION_DELAY, e -> updateAnimation());
        animationTimer.setRepeats(true);
    }

    /**
     * 定位窗口 - 从电脑屏幕右下角飞出，停靠在应用主窗口的右下角
     */
    private void positionWindowOffScreen(JFrame parent) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;

        // 初始位置：屏幕右下角外
        currentX = screenWidth - window.getWidth() - 20;
        currentY = screenHeight;

        if (parent != null && parent.isVisible()) {
            // 计算停靠位置：应用主窗口的右下角
            Rectangle parentBounds = parent.getBounds();

            // 目标X位置：主窗口右边缘 - 通知宽度 - 20px边距
            targetX = parentBounds.x + parentBounds.width - window.getWidth() - 20;

            // 确保不超出屏幕右边缘
            if (targetX + window.getWidth() > screenWidth) {
                targetX = screenWidth - window.getWidth() - 20;
            }
            // 确保不超出屏幕左边缘
            if (targetX < 20) {
                targetX = 20;
            }

            // 目标Y位置：主窗口底边 - 通知窗口高度 - 60px边距
            targetY = parentBounds.y + parentBounds.height - window.getHeight() - 60;

            // 确保不超出屏幕底部
            if (targetY + window.getHeight() > screenHeight) {
                targetY = screenHeight - window.getHeight() - 60;
            }
            // 确保不超出主窗口顶部
            if (targetY < parentBounds.y + 60) {
                targetY = parentBounds.y + 60;
            }
        } else {
            // 如果没有父窗口或父窗口不可见，使用屏幕右下角
            targetX = screenWidth - window.getWidth() - 20;
            targetY = screenHeight - window.getHeight() - 60;
        }

        window.setLocation(currentX, currentY);
    }

    /**
     * 统一的动画更新方法（使用缓动函数）
     */
    private void updateAnimation() {
        if (isSlideIn) {
            // 滑入动画 - 同时移动X和Y
            boolean xReached = Math.abs(currentX - targetX) <= 1;
            boolean yReached = Math.abs(currentY - targetY) <= 1;

            if (!xReached || !yReached) {
                // X方向的缓动
                if (!xReached) {
                    int distanceX = (int) ((targetX - currentX) * EASING_FACTOR);
                    if (distanceX == 0) {
                        distanceX = currentX > targetX ? -1 : 1;
                    }
                    currentX += distanceX;
                }

                // Y方向的缓动
                if (!yReached) {
                    int distanceY = (int) ((targetY - currentY) * EASING_FACTOR);
                    if (distanceY == 0) {
                        distanceY = currentY > targetY ? -1 : 1;
                    }
                    currentY += distanceY;
                }

                window.setLocation(currentX, currentY);
            } else {
                // 到达目标位置
                currentX = targetX;
                currentY = targetY;
                window.setLocation(currentX, currentY);
                animationTimer.stop();
            }
        } else {
            // 滑出动画 - 回到屏幕右下角外
            int targetOutX = screenWidth - window.getWidth() - 20;
            int targetOutY = screenHeight;

            boolean xReached = currentX >= targetOutX || Math.abs(currentX - targetOutX) <= 1;
            boolean yReached = currentY >= targetOutY;

            if (!xReached || !yReached) {
                // X方向的缓动（移动到屏幕右侧）
                if (!xReached) {
                    int distanceX = (int) ((targetOutX - currentX) * EASING_FACTOR);
                    if (distanceX == 0) {
                        distanceX = 1;
                    }
                    currentX += distanceX;
                }

                // Y方向的缓动（移动到屏幕底部外）
                if (!yReached) {
                    int distanceY = (int) ((targetOutY - currentY) * EASING_FACTOR) + 1;
                    currentY += distanceY;
                }

                window.setLocation(currentX, currentY);
            } else {
                // 滑出完成，关闭窗口
                animationTimer.stop();
                window.dispose();
            }
        }
    }

    /**
     * 开始滑出动画
     */
    private void slideOut() {
        autoCloseTimer.stop();
        isSlideIn = false;
        targetY = screenHeight; // 滑出到屏幕外
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    /**
     * 创建现代化通知面板
     */
    private JPanel createNotificationPanel(UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        // 主面板 - 使用自定义绘制实现圆角和阴影
        JPanel panel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // 绘制阴影
                for (int i = 4; i > 0; i--) {
                    int alpha = 15 - i * 3;
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillRoundRect(i, i, width - i * 2, height - i * 2, 16, 16);
                }

                // 绘制白色圆角背景
                g2.setColor(ModernColors.BG_WHITE);
                g2.fillRoundRect(0, 0, width, height, 12, 12);

                // 绘制顶部装饰条（使用蓝色渐变）
                GradientPaint topStrip = new GradientPaint(
                        0, 0, ModernColors.PRIMARY,
                        width, 0, ModernColors.SECONDARY
                );
                g2.setPaint(topStrip);
                // 使用裁剪确保圆角
                Shape oldClip = g2.getClip();
                g2.setClip(0, 0, width, 3);
                g2.fillRoundRect(0, 0, width, 12, 12, 12);
                g2.setClip(oldClip);

                // 绘制边框
                g2.setColor(ModernColors.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(380, 160));

        // 顶部区域：包含图标、内容和关闭按钮
        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setOpaque(false);

        // 左侧图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 36, 36));
        topPanel.add(iconLabel, BorderLayout.WEST);

        // 中心内容区域
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 标题
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));
        titleLabel.setForeground(ModernColors.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 版本信息
        JLabel versionLabel = new JLabel(String.format("%s → %s",
                updateInfo.getCurrentVersion(), updateInfo.getLatestVersion()));
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        versionLabel.setForeground(ModernColors.PRIMARY);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 简短描述
        String summary = extractSummary(updateInfo);
        JLabel summaryLabel = new JLabel("<html><body style='width: 260px'>" + summary + "</body></html>");
        summaryLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        summaryLabel.setForeground(ModernColors.TEXT_HINT);
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(summaryLabel);

        topPanel.add(contentPanel, BorderLayout.CENTER);

        // 右上角关闭按钮
        JButton closeButton = createCloseButton();
        JPanel closePanel = new JPanel(new BorderLayout());
        closePanel.setOpaque(false);
        closePanel.add(closeButton, BorderLayout.NORTH);
        topPanel.add(closePanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.CENTER);

        // 底部按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        JButton laterButton = createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.addActionListener(e -> slideOut());

        JButton viewButton = createPrimaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_VIEW_DETAILS));
        viewButton.addActionListener(e -> {
            animationTimer.stop();
            window.dispose();
            autoCloseTimer.stop();
            onViewDetails.accept(updateInfo);
        });

        buttonPanel.add(laterButton);
        buttonPanel.add(viewButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 点击面板也可以查看详情（但不包括按钮区域）
        topPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    animationTimer.stop();
                    window.dispose();
                    autoCloseTimer.stop();
                    onViewDetails.accept(updateInfo);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // 微妙的悬停效果
                topPanel.setOpaque(false);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                topPanel.setOpaque(false);
            }
        });

        return panel;
    }

    /**
     * 创建关闭按钮
     */
    private JButton createCloseButton() {
        JButton button = new JButton("×");
        button.setFont(new Font("Arial", Font.PLAIN, 22));
        button.setForeground(ModernColors.TEXT_HINT);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(24, 24));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(ModernColors.TEXT_PRIMARY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(ModernColors.TEXT_HINT);
            }
        });

        button.addActionListener(e -> {
            autoCloseTimer.stop();
            slideOut();
        });

        return button;
    }

    /**
     * 创建主按钮（查看详情）
     */
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制蓝色渐变背景
                GradientPaint gradient = new GradientPaint(
                        0, 0, getModel().isRollover() ? ModernColors.PRIMARY_DARK : ModernColors.PRIMARY,
                        getWidth(), 0, getModel().isRollover() ? ModernColors.SECONDARY_DARK : ModernColors.SECONDARY
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 18, 8, 18));
        button.setOpaque(false);

        return button;
    }

    /**
     * 创建次级按钮（稍后提醒）
     */
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制背景
                if (getModel().isRollover()) {
                    g2.setColor(ModernColors.HOVER_BG);
                } else {
                    g2.setColor(ModernColors.BG_LIGHT);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        button.setForeground(ModernColors.TEXT_SECONDARY);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 18, 8, 18));
        button.setOpaque(false);

        return button;
    }

    private String extractSummary(UpdateInfo updateInfo) {
        if (updateInfo.getReleaseInfo() == null) {
            return I18nUtil.isChinese() ? "点击查看详情" : "Click for details";
        }

        String body = updateInfo.getReleaseInfo().getStr("body", "");
        if (body.isEmpty()) {
            return I18nUtil.isChinese() ? "包含新功能和改进" : "New features and improvements";
        }

        // 提取第一行或前60个字符（更短，确保不会被遮盖）
        String cleaned = body.trim()
                .replaceAll("^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1")
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // 使用更短的长度限制（60字符），确保在通知窗口中显示完整
        int maxLength = 60;
        if (cleaned.length() > maxLength) {
            int lastSpace = cleaned.lastIndexOf(' ', maxLength);
            if (lastSpace > maxLength * 0.7) {
                cleaned = cleaned.substring(0, lastSpace) + "...";
            } else {
                cleaned = cleaned.substring(0, maxLength) + "...";
            }
        }

        if (cleaned.isEmpty()) {
            return I18nUtil.isChinese() ? "包含新功能和改进" : "New features and improvements";
        }
        return cleaned;
    }

    /**
     * 显示通知（从右下角滑入）
     */
    public void show() {
        window.setVisible(true);
        isSlideIn = true;
        animationTimer.start();
        autoCloseTimer.start();
    }

    /**
     * 关闭通知
     */
    public void dispose() {
        autoCloseTimer.stop();
        animationTimer.stop();
        window.dispose();
    }
}

