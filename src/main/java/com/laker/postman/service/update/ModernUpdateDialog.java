package com.laker.postman.service.update;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化更新对话框 - 简洁清晰的更新提示
 */
public class ModernUpdateDialog extends JDialog {

    private int userChoice = -1; // 0=手动下载, 1=自动更新, 2=稍后

    public ModernUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE), true);

        initComponents(updateInfo);

        setSize(600, 520);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout());

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);

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
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(new Color(247, 250, 252));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // 图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 56, 56));
        panel.add(iconLabel, BorderLayout.WEST);

        // 版本信息
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel versionLabel = new JLabel(String.format(
                "%s %s → %s",
                I18nUtil.isChinese() ? "版本" : "Version",
                updateInfo.getCurrentVersion(),
                updateInfo.getLatestVersion()
        ));
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 14));
        versionLabel.setForeground(new Color(0, 122, 255));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 发布时间
        String publishedAt = updateInfo.getReleaseInfo() != null ?
                updateInfo.getReleaseInfo().getStr("published_at", "") : "";
        if (!publishedAt.isEmpty()) {
            String dateStr = publishedAt.substring(0, 10); // 提取日期部分
            JLabel dateLabel = new JLabel(
                    (I18nUtil.isChinese() ? "发布于 " : "Released on ") + dateStr
            );
            dateLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
            dateLabel.setForeground(new Color(150, 150, 150));
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(titleLabel);
            infoPanel.add(Box.createVerticalStrut(6));
            infoPanel.add(versionLabel);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(dateLabel);
        } else {
            infoPanel.add(titleLabel);
            infoPanel.add(Box.createVerticalStrut(8));
            infoPanel.add(versionLabel);
        }

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChangelogPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(0, 24, 16, 24));

        // 标题
        JLabel titleLabel = new JLabel("📝 " + (I18nUtil.isChinese() ? "更新内容" : "What's New"));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 15));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 更新日志
        String changelog = extractChangelog(updateInfo.getReleaseInfo());
        JTextArea textArea = new JTextArea(changelog);
        textArea.setEditable(false);
        textArea.setFocusable(false); // 禁用焦点，避免出现光标
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        textArea.setBackground(new Color(252, 252, 252));
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String extractChangelog(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return I18nUtil.isChinese() ?
                    "暂无详细更新说明，请访问发布页面查看。" :
                    "No detailed release notes available.";
        }

        String body = releaseInfo.getStr("body");
        if (StrUtil.isBlank(body)) {
            return I18nUtil.isChinese() ?
                    "包含新功能、改进和错误修复。" :
                    "Includes new features, improvements and bug fixes.";
        }

        // 清理 Markdown 但保留基本结构
        String cleaned = body.trim()
                .replaceAll("^#{1,6}\\s+", "▸ ")  // 标题
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
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 24, 20, 24));

        // 左侧提示
        JLabel tipLabel = new JLabel(I18nUtil.isChinese() ?
                "💡 建议在更新前保存工作" :
                "💡 Save your work before updating");
        tipLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        tipLabel.setForeground(new Color(150, 150, 150));
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

        JButton autoButton = createPrimaryButton(
                I18nUtil.isChinese() ? "立即更新" : "Update Now"
        );
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
        button.setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 122, 255));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 20, 8, 20));

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
        button.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        button.setForeground(new Color(100, 100, 100));
        button.setBackground(new Color(248, 248, 248));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 20, 8, 20));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(238, 238, 238));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(248, 248, 248));
            }
        });

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

