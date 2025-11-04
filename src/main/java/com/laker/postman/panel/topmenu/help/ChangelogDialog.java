package com.laker.postman.panel.topmenu.help;

import com.laker.postman.service.update.changelog.ChangelogService;
import com.laker.postman.service.update.source.UpdateSource;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

/**
 * 版本更新日志对话框
 *
 * <p>重构后的版本：使用统一的更新源架构，消除代码重复</p>
 */
@Slf4j
public class ChangelogDialog extends JDialog {

    private final JTextArea contentArea;
    private final JButton refreshButton;
    private final JLabel statusLabel;
    private final ChangelogService changelogService;

    public ChangelogDialog(Frame parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.CHANGELOG_TITLE), true);

        this.changelogService = new ChangelogService();

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部面板：当前版本信息
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 中间面板：更新日志内容
        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 13));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        add(scrollPane, BorderLayout.CENTER);

        // 底部面板：按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // 状态标签
        statusLabel = new JLabel(" ");
        statusLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        statusLabel.setForeground(Color.GRAY);
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        refreshButton = new JButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_REFRESH));
        refreshButton.addActionListener(e -> loadChangelog());

        JButton githubButton = new JButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITHUB));
        githubButton.addActionListener(e -> openGitHubWeb());

        JButton giteeButton = new JButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITEE));
        giteeButton.addActionListener(e -> openGiteeWeb());

        JButton closeButton = new JButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_CLOSE));
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(refreshButton);
        buttonPanel.add(githubButton);
        buttonPanel.add(giteeButton);
        buttonPanel.add(closeButton);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 设置对话框属性
        setSize(750, 650);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 异步加载更新日志
        loadChangelog();
    }

    /**
     * 创建顶部面板
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        String currentVersion = SystemUtil.getCurrentVersion();
        JLabel versionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CHANGELOG_CURRENT_VERSION, currentVersion));
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));

        panel.add(versionLabel, BorderLayout.WEST);

        return panel;
    }

    /**
     * 加载更新日志
     */
    private void loadChangelog() {
        contentArea.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));
        refreshButton.setEnabled(false);
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return changelogService.getChangelog();
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if (result != null) {
                        contentArea.setText(result);
                        contentArea.setCaretPosition(0); // 滚动到顶部
                        statusLabel.setText(" ");
                    } else {
                        String errorMsg = I18nUtil.getMessage(MessageKeys.CHANGELOG_LOAD_FAILED,
                                I18nUtil.getMessage(MessageKeys.ERROR_NETWORK_TIMEOUT));
                        contentArea.setText(errorMsg);
                        statusLabel.setText(errorMsg);
                    }
                } catch (Exception e) {
                    log.error("Failed to load changelog", e);
                    String errorMsg = I18nUtil.getMessage(MessageKeys.CHANGELOG_LOAD_FAILED, e.getMessage());
                    contentArea.setText(errorMsg);
                    statusLabel.setText(errorMsg);
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /**
     * 打开 GitHub 网页版
     */
    private void openGitHubWeb() {
        UpdateSource github = changelogService.getSourceSelector().selectBestSource();
        // 如果当前不是 GitHub，获取 GitHub 实例
        if (!"GitHub".equals(github.getName())) {
            github = changelogService.getSourceSelector().getFallbackSource(github);
        }
        openUrl(github.getWebUrl());
    }

    /**
     * 打开 Gitee 网页版
     */
    private void openGiteeWeb() {
        UpdateSource source = changelogService.getSourceSelector().selectBestSource();
        // 如果当前不是 Gitee，获取 Gitee 实例
        if (!"Gitee".equals(source.getName())) {
            source = changelogService.getSourceSelector().getFallbackSource(source);
        }
        openUrl(source.getWebUrl());
    }

    /**
     * 打开 URL
     */
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            log.error("Failed to open URL: {}", url, e);
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, e.getMessage()),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 显示更新日志对话框
     */
    public static void showDialog(Frame parent) {
        SwingUtilities.invokeLater(() -> {
            ChangelogDialog dialog = new ChangelogDialog(parent);
            dialog.setVisible(true);
        });
    }
}

