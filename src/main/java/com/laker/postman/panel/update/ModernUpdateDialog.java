package com.laker.postman.panel.update;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 现代化更新对话框 - 简洁清晰的更新提示
 */
public class ModernUpdateDialog extends JDialog {

    private int userChoice = -1; // 0=手动下载, 1=自动更新, 2=稍后

    public ModernUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE), true);

        initComponents(updateInfo);

        setSize(600, 380);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout());

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(ModernColors.BG_WHITE);

        // 头部
        JPanel headerPanel = createHeaderPanel(updateInfo);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 更新日志
        JPanel changelogPanel = createChangelogPanel(updateInfo);
        mainPanel.add(changelogPanel, BorderLayout.CENTER);

        // 按钮
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeaderPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制蓝色渐变背景
                GradientPaint gradient = new GradientPaint(0, 0, ModernColors.PRIMARY_LIGHTER,
                        getWidth(), getHeight(), ModernColors.SECONDARY_LIGHTER
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // 绘制装饰性光晕（蓝色）
                g2.setColor(ModernColors.primaryWithAlpha(20));
                g2.fillOval(-50, -50, 200, 200);
                g2.fillOval(getWidth() - 150, getHeight() - 100, 200, 150);

                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // 图标 - 使用更大的尺寸
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 64, 64));
        panel.add(iconLabel, BorderLayout.WEST);

        // 版本信息
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +8));
        titleLabel.setForeground(ModernColors.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel versionLabel = new JLabel(String.format("%s %s → %s", I18nUtil.isChinese() ? "版本" : "Version", updateInfo.getCurrentVersion(), updateInfo.getLatestVersion()));
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +3));
        versionLabel.setForeground(ModernColors.PRIMARY);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 发布时间
        String publishedAt = updateInfo.getReleaseInfo() != null ? updateInfo.getReleaseInfo().getStr("published_at", "") : "";
        if (!publishedAt.isEmpty()) {
            String dateStr = publishedAt.substring(0, 10); // 提取日期部分
            JLabel dateLabel = new JLabel((I18nUtil.isChinese() ? "发布于 " : "Released on ") + dateStr);
            dateLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
            dateLabel.setForeground(ModernColors.TEXT_HINT);
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(titleLabel);
            infoPanel.add(Box.createVerticalStrut(8));
            infoPanel.add(versionLabel);
            infoPanel.add(Box.createVerticalStrut(6));
            infoPanel.add(dateLabel);
        } else {
            infoPanel.add(titleLabel);
            infoPanel.add(Box.createVerticalStrut(10));
            infoPanel.add(versionLabel);
        }

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChangelogPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(ModernColors.BG_WHITE);
        panel.setBorder(new EmptyBorder(0, 24, 16, 24));

        // 标题
        JLabel titleLabel = new JLabel("📝 " + (I18nUtil.isChinese() ? "更新内容" : "What's New"));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, +3));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 更新日志
        String changelog = extractChangelog(updateInfo.getReleaseInfo());
        JTextArea textArea = new JTextArea(changelog);
        textArea.setEditable(false);
        textArea.setFocusable(false); // 禁用焦点，避免出现光标
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        textArea.setBackground(ModernColors.BG_LIGHT);
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.BORDER_LIGHT, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String extractChangelog(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return I18nUtil.isChinese() ? "暂无详细更新说明，请访问发布页面查看。" : "No detailed release notes available.";
        }

        String body = releaseInfo.getStr("body");
        if (StrUtil.isBlank(body)) {
            return I18nUtil.isChinese() ? "包含新功能、改进和错误修复。" : "Includes new features, improvements and bug fixes.";
        }

        // 清理 Markdown 但保留基本结构
        String cleaned = body.trim().replaceAll("^#{1,6}\\s+", "▸ ")  // 标题
                .replaceAll("(?m)^-\\s+", "  • ")  // 列表
                .replaceAll("(?m)^\\*\\s+", "  • ")  // 列表
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // 粗体
                .replaceAll("\\*(.+?)\\*", "$1")  // 斜体
                .replaceAll("```[\\s\\S]*?```", "[代码示例]")  // 代码块
                .replaceAll("`(.+?)`", "$1")  // 行内代码
                .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1")  // 链接
                .replaceAll("\\n{3,}", "\n\n");  // 多个空行

        return cleaned;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernColors.BG_WHITE);
        panel.setBorder(new EmptyBorder(16, 24, 20, 24));

        // 左侧提示
        JLabel tipLabel = new JLabel(I18nUtil.isChinese() ? "💡 建议在更新前保存工作" : "💡 Save your work before updating");
        tipLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        tipLabel.setForeground(ModernColors.TEXT_HINT);
        panel.add(tipLabel, BorderLayout.WEST);

        // 右侧按钮
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonsPanel.setOpaque(false);

        JButton laterButton = createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.addActionListener(e -> {
            userChoice = 2;
            dispose();
        });

        JButton manualButton = createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_MANUAL_DOWNLOAD));
        manualButton.addActionListener(e -> {
            userChoice = 0;
            dispose();
        });

        JButton autoButton = createPrimaryButton(I18nUtil.isChinese() ? "立即更新" : "Update Now");
        autoButton.addActionListener(e -> {
            userChoice = 1;
            dispose();
        });

        buttonsPanel.add(laterButton);
        buttonsPanel.add(manualButton);
        buttonsPanel.add(autoButton);

        panel.add(buttonsPanel, BorderLayout.EAST);

        // 设置默认按钮
        getRootPane().setDefaultButton(autoButton);

        return panel;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(new EmptyBorder(8, 20, 8, 20));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(new EmptyBorder(8, 20, 8, 20));
        return button;
    }

    public int showDialogAndGetChoice() {
        setVisible(true);
        return userChoice;
    }

    public static int showUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        ModernUpdateDialog dialog = new ModernUpdateDialog(parent, updateInfo);
        return dialog.showDialogAndGetChoice();
    }
}

