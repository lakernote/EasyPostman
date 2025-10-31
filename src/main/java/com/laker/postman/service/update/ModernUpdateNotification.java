package com.laker.postman.service.update;

import com.formdev.flatlaf.extras.FlatSVGIcon;
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
 */
public class ModernUpdateNotification {

    private final JWindow window;
    private final Timer autoCloseTimer;
    private final Timer slideInTimer;
    private int currentY;
    private int targetY;
    private static final int ANIMATION_STEPS = 20;
    private static final int ANIMATION_DELAY = 10;

    public ModernUpdateNotification(JFrame parent, UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        window = new JWindow(parent);
        window.setAlwaysOnTop(true);

        JPanel panel = createNotificationPanel(updateInfo, onViewDetails);
        window.setContentPane(panel);
        window.pack();

        // 初始位置设在屏幕外（下方）
        positionWindowOffScreen(parent);

        // 自动关闭定时器（5秒）
        autoCloseTimer = new Timer(5000, e -> slideOut());
        autoCloseTimer.setRepeats(false);

        // 滑入动画
        slideInTimer = new Timer(ANIMATION_DELAY, e -> animateSlideIn());
        slideInTimer.setRepeats(true);
    }

    private void positionWindowOffScreen(JFrame parent) {
        Rectangle parentBounds = parent.getBounds();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int x = Math.min(
                parentBounds.x + parentBounds.width - window.getWidth() - 20,
                screenSize.width - window.getWidth() - 20
        );

        targetY = Math.min(
                parentBounds.y + parentBounds.height - window.getHeight() - 60,
                screenSize.height - window.getHeight() - 60
        );

        currentY = screenSize.height; // 从屏幕底部开始
        window.setLocation(x, currentY);
    }

    private void animateSlideIn() {
        if (currentY > targetY) {
            int step = Math.max(1, (currentY - targetY) / ANIMATION_STEPS);
            currentY -= step;
            window.setLocation(window.getX(), currentY);
        } else {
            currentY = targetY;
            window.setLocation(window.getX(), currentY);
            slideInTimer.stop();
        }
    }

    private void slideOut() {
        Timer slideOutTimer = new Timer(ANIMATION_DELAY, null);
        slideOutTimer.addActionListener(e -> {
            currentY += (Toolkit.getDefaultToolkit().getScreenSize().height - currentY) / ANIMATION_STEPS + 1;
            window.setLocation(window.getX(), currentY);

            if (currentY >= Toolkit.getDefaultToolkit().getScreenSize().height) {
                slideOutTimer.stop();
                window.dispose();
            }
        });
        slideOutTimer.start();
    }

    private JPanel createNotificationPanel(UpdateInfo updateInfo, Consumer<UpdateInfo> onViewDetails) {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(16, 16, 16, 16)
        ));
        panel.setPreferredSize(new Dimension(380, 140));

        // 左侧图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 40, 40));
        panel.add(iconLabel, BorderLayout.WEST);

        // 中心内容
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 标题
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 版本信息
        JLabel versionLabel = new JLabel(String.format("%s → %s",
                updateInfo.getCurrentVersion(), updateInfo.getLatestVersion()));
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        versionLabel.setForeground(new Color(0, 122, 255));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 简短描述 - 使用JLabel单行显示，避免文本被遮盖
        String summary = extractSummary(updateInfo);
        JLabel summaryLabel = new JLabel(summary);
        summaryLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        summaryLabel.setForeground(new Color(120, 120, 120));
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(summaryLabel);

        panel.add(contentPanel, BorderLayout.CENTER);

        // 右侧关闭按钮
        JButton closeButton = createCloseButton();
        JPanel topRightPanel = new JPanel(new BorderLayout());
        topRightPanel.setOpaque(false);
        topRightPanel.add(closeButton, BorderLayout.NORTH);
        panel.add(topRightPanel, BorderLayout.EAST);

        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        JButton laterButton = createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.addActionListener(e -> slideOut());

        JButton viewButton = createPrimaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_VIEW_DETAILS));
        viewButton.addActionListener(e -> {
            window.dispose();
            autoCloseTimer.stop();
            onViewDetails.accept(updateInfo);
        });

        buttonPanel.add(laterButton);
        buttonPanel.add(viewButton);

        // 添加底部按钮到内容面板
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(buttonPanel);

        // 点击面板也可以查看详情
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    window.dispose();
                    autoCloseTimer.stop();
                    onViewDetails.accept(updateInfo);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(248, 249, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(Color.WHITE);
            }
        });

        return panel;
    }

    private JButton createCloseButton() {
        JButton button = new JButton("×");
        button.setFont(new Font("Arial", Font.PLAIN, 20));
        button.setForeground(new Color(150, 150, 150));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(24, 24));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(new Color(150, 150, 150));
            }
        });

        button.addActionListener(e -> {
            autoCloseTimer.stop();
            slideOut();
        });

        return button;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 122, 255));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(6, 16, 6, 16));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0, 100, 220));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(0, 122, 255));
            }
        });

        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        button.setForeground(new Color(100, 100, 100));
        button.setBackground(new Color(245, 245, 245));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(6, 16, 6, 16));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(235, 235, 235));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(245, 245, 245));
            }
        });

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

    public void show() {
        window.setVisible(true);
        slideInTimer.start();
        autoCloseTimer.start();
    }

    public void dispose() {
        autoCloseTimer.stop();
        slideInTimer.stop();
        window.dispose();
    }
}

