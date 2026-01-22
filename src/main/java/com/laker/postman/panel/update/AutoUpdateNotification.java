package com.laker.postman.panel.update;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * 自动更新通知弹窗 - 位于主窗口右下角，自动淡入淡出
 */
@Slf4j
public class AutoUpdateNotification {

    private static final int NOTIFICATION_WIDTH = 400;
    private static final int NOTIFICATION_HEIGHT = 140;
    private static final int MARGIN = 20;
    private static final int FADE_DURATION = 300; // 毫秒
    private static final int DISPLAY_DURATION = 5000; // 毫秒
    private static final float FADE_STEP = 0.05f; // 淡入淡出步长
    private static final int FADE_TIMER_DELAY = 10; // 淡入淡出定时器延迟（毫秒）
    private final JDialog dialog;
    private Timer fadeTimer;
    private Timer autoCloseTimer;
    private float opacity = 0f; // 由于所有操作都在 EDT 线程，不需要 volatile

    private AutoUpdateNotification(JFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        // 创建非模态、无装饰的对话框
        dialog = new JDialog(parent, false);
        dialog.setUndecorated(true);
        dialog.setFocusableWindowState(false);
        dialog.setType(Window.Type.UTILITY);
        dialog.setSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);

        // 初始透明度
        dialog.setOpacity(0f);

        // 创建通知内容
        JPanel contentPanel = createNotificationPanel(updateInfo, onViewDetails);
        dialog.setContentPane(contentPanel);

        // 定位到父窗口右下角
        positionDialog(parent);
    }

    /**
     * 显示更新通知
     */
    public static void show(JFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        SwingUtilities.invokeLater(() -> {
            // 只在父窗口可见且获得焦点时显示
            if (parent == null || !parent.isVisible()) {
                log.debug("Parent window is not visible, skip showing notification");
                return;
            }

            if (!parent.isFocused() && !parent.isActive()) {
                log.debug("Parent window is not active, skip showing notification");
                return;
            }

            AutoUpdateNotification notification = new AutoUpdateNotification(parent, updateInfo, onViewDetails);
            notification.display();
        });
    }

    /**
     * 显示通知（带淡入淡出动画）
     */
    private void display() {
        dialog.setVisible(true);

        // 淡入动画
        fadeIn();

        // 自动关闭定时器
        autoCloseTimer = new Timer(DISPLAY_DURATION, e -> fadeOut());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    /**
     * 淡入动画
     */
    private void fadeIn() {
        stopFadeTimer(); // 停止之前的淡入淡出定时器

        fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            opacity += FADE_STEP;
            if (opacity >= 1.0f) {
                opacity = 1.0f;
                stopFadeTimer();
            }
            dialog.setOpacity(opacity);
        });
        fadeTimer.start();
    }

    /**
     * 淡出动画
     */
    private void fadeOut() {
        stopFadeTimer(); // 停止之前的淡入淡出定时器
        stopAutoCloseTimer(); // 停止自动关闭定时器

        fadeTimer = new Timer(FADE_TIMER_DELAY, null);
        fadeTimer.addActionListener(e -> {
            opacity -= FADE_STEP;
            if (opacity <= 0f) {
                opacity = 0f;
                stopFadeTimer();
                cleanupAndClose();
            }
            dialog.setOpacity(opacity);
        });
        fadeTimer.start();
    }

    /**
     * 停止淡入淡出定时器
     */
    private void stopFadeTimer() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
    }

    /**
     * 停止自动关闭定时器
     */
    private void stopAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
            autoCloseTimer = null;
        }
    }

    /**
     * 清理资源并关闭对话框
     */
    private void cleanupAndClose() {
        stopFadeTimer();
        stopAutoCloseTimer();
        if (dialog != null) {
            dialog.dispose();
        }
    }

    /**
     * 定位对话框到父窗口右下角
     */
    private void positionDialog(JFrame parent) {
        Rectangle parentBounds = parent.getBounds();

        int x = parentBounds.x + parentBounds.width - NOTIFICATION_WIDTH - MARGIN;
        int y = parentBounds.y + parentBounds.height - NOTIFICATION_HEIGHT - MARGIN - 40; // 留出底部空间

        // 确保不超出屏幕边界
        GraphicsConfiguration gc = parent.getGraphicsConfiguration();
        if (gc != null) {
            Rectangle screenBounds = gc.getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

            int maxX = screenBounds.x + screenBounds.width - screenInsets.right - NOTIFICATION_WIDTH - MARGIN;
            int maxY = screenBounds.y + screenBounds.height - screenInsets.bottom - NOTIFICATION_HEIGHT - MARGIN;

            x = Math.min(x, maxX);
            y = Math.min(y, maxY);
            x = Math.max(screenBounds.x + screenInsets.left + MARGIN, x);
            y = Math.max(screenBounds.y + screenInsets.top + MARGIN, y);
        }

        dialog.setLocation(x, y);
    }

    /**
     * 创建通知面板
     */
    private JPanel createNotificationPanel(UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor(), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        // 左侧图标
        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/info.svg", 36, 36));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        panel.add(iconLabel, BorderLayout.WEST);

        // 中心内容
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 标题
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 版本信息
        String versionText = String.format("%s → %s",
                updateInfo.getCurrentVersion(),
                updateInfo.getLatestVersion());
        JLabel versionLabel = new JLabel(versionText);
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 描述
        String description = extractDescription(updateInfo);
        JLabel descLabel = new JLabel("<html><body style='width: 240px'>" + description + "</body></html>");
        descLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(descLabel);

        panel.add(contentPanel, BorderLayout.CENTER);

        // 右侧按钮区域
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        // 关闭按钮
        JButton closeButton = createCloseButton();
        closeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // 查看详情按钮
        JButton viewButton = createViewDetailsButton(updateInfo, onViewDetails);
        viewButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

        buttonPanel.add(closeButton);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(viewButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建关闭按钮
     */
    private JButton createCloseButton() {
        JButton button = new JButton("×");
        button.setFont(new Font("Arial", Font.PLAIN, 20));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.addActionListener(e -> fadeOut());
        return button;
    }

    /**
     * 创建查看详情按钮
     */
    private JButton createViewDetailsButton(UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        JButton button = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_VIEW_DETAILS));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            fadeOut();
            // 延迟调用，等待淡出动画完成
            Timer delayTimer = new Timer(FADE_DURATION, evt -> onViewDetails.accept(updateInfo));
            delayTimer.setRepeats(false);
            delayTimer.start();
        });
        return button;
    }


    /**
     * 提取更新描述
     */
    private String extractDescription(UpdateInfo updateInfo) {
        if (updateInfo.getReleaseInfo() == null) {
            return I18nUtil.isChinese() ? "点击查看更新详情" : "Click to view details";
        }

        String body = updateInfo.getReleaseInfo().getStr("body", "");
        if (body.isEmpty()) {
            return I18nUtil.isChinese() ? "包含新功能和改进" : "New features and improvements";
        }

        // 清理 Markdown 格式
        String cleaned = body.trim()
                .replaceAll("^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)]\\([^)]+\\)", "$1") // 优化正则表达式
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // 截取前60个字符
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
}

