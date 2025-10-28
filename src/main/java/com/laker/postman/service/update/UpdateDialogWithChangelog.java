package com.laker.postman.service.update;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 带更新日志的更新对话框
 * 参考 VS Code、IntelliJ IDEA 等项目，在升级提示中展示最新的更新内容
 */
@Slf4j
public class UpdateDialogWithChangelog extends JDialog {

    private static final int MAX_CHANGELOG_LENGTH = 500; // 最大显示字符数

    private int userChoice = -1; // 0=手动下载, 1=自动下载, 2=稍后提醒

    public UpdateDialogWithChangelog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE), true);

        initComponents(updateInfo);

        setSize(650, 550);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout(0, 0));

        // 顶部面板：图标和版本信息
        JPanel topPanel = createTopPanel(updateInfo);
        add(topPanel, BorderLayout.NORTH);

        // 中间面板：更新日志
        JPanel centerPanel = createChangelogPanel(updateInfo);
        add(centerPanel, BorderLayout.CENTER);

        // 底部面板：操作按钮
        JPanel bottomPanel = createButtonPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(20, 20, 15, 20));
        panel.setBackground(new Color(245, 250, 255));

        // 左侧图标
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/info.svg", 48, 48));
        panel.add(iconLabel, BorderLayout.WEST);

        // 右侧版本信息
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel versionLabel = new JLabel(String.format("%s → %s",
                updateInfo.getCurrentVersion(),
                updateInfo.getLatestVersion()));
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 14));
        versionLabel.setForeground(new Color(0, 120, 215));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(versionLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChangelogPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 20, 15, 20));

        // 标题
        JLabel titleLabel = new JLabel("📝 " + getWhatsNewTitle());
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 更新日志内容
        String changelog = extractChangelog(updateInfo.getReleaseInfo());
        JTextArea changelogArea = new JTextArea(changelog);
        changelogArea.setEditable(false);
        changelogArea.setLineWrap(true);
        changelogArea.setWrapStyleWord(true);
        changelogArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        changelogArea.setBackground(new Color(250, 250, 250));
        changelogArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(changelogArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        scrollPane.setPreferredSize(new Dimension(600, 300));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private String getWhatsNewTitle() {
        return I18nUtil.isChinese() ? "更新内容" : "What's New";
    }

    private String extractChangelog(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return getNoChangelogMessage();
        }

        String body = releaseInfo.getStr("body");
        if (StrUtil.isBlank(body)) {
            return getNoChangelogMessage();
        }

        // 清理 Markdown 格式，保留可读性
        String cleaned = body.trim()
                .replaceAll("^#{1,6}\\s+", "• ")  // 标题转为列表
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // 移除粗体标记
                .replaceAll("\\*(.+?)\\*", "$1")  // 移除斜体标记
                .replaceAll("```[\\s\\S]*?```", "")  // 移除代码块
                .replaceAll("`(.+?)`", "$1")  // 移除行内代码标记
                .replaceAll("\\[(.+?)]\\(.+?\\)", "$1")  // 链接只保留文本
                .replaceAll("\\n{3,}", "\n\n");  // 压缩多个空行

        // 如果内容太长，截断并添加省略号
        if (cleaned.length() > MAX_CHANGELOG_LENGTH) {
            cleaned = cleaned.substring(0, MAX_CHANGELOG_LENGTH) + "\n\n...";
        }

        return cleaned;
    }

    private String getNoChangelogMessage() {
        return I18nUtil.isChinese()
                ? "暂无详细更新说明，请访问发布页面查看完整信息。"
                : "No detailed release notes available. Please visit the release page for more information.";
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setBorder(new EmptyBorder(10, 20, 20, 20));

        // 稍后提醒按钮
        JButton laterButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        laterButton.addActionListener(e -> {
            userChoice = 2;
            dispose();
        });

        // 手动下载按钮
        JButton manualButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_MANUAL_DOWNLOAD));
        manualButton.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        manualButton.addActionListener(e -> {
            userChoice = 0;
            dispose();
        });

        // 自动更新按钮（默认选项，高亮显示）
        JButton autoButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_AUTO_DOWNLOAD));
        autoButton.setFont(FontsUtil.getDefaultFont(Font.BOLD, 13));
        autoButton.setForeground(Color.WHITE);
        autoButton.setBackground(new Color(0, 120, 215));
        autoButton.setOpaque(true);
        autoButton.setBorderPainted(false);
        autoButton.setFocusPainted(false);
        autoButton.addActionListener(e -> {
            userChoice = 1;
            dispose();
        });

        panel.add(laterButton);
        panel.add(manualButton);
        panel.add(autoButton);

        // 设置默认按钮
        getRootPane().setDefaultButton(autoButton);

        return panel;
    }

    /**
     * 显示对话框并返回用户选择
     * @return 0=手动下载, 1=自动下载, 2=稍后提醒, -1=关闭对话框
     */
    public int showDialogAndGetChoice() {
        setVisible(true);
        return userChoice;
    }

    /**
     * 静态方法：显示更新对话框并返回用户选择
     */
    public static int showUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        UpdateDialogWithChangelog dialog = new UpdateDialogWithChangelog(parent, updateInfo);
        return dialog.showDialogAndGetChoice();
    }
}

