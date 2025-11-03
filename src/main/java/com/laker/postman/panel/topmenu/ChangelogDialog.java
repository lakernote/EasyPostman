package com.laker.postman.panel.topmenu;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.panel.topmenu.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 版本更新日志对话框
 * 参考 VS Code、IntelliJ IDEA 等开源系统的更新日志展示方式
 */
@Slf4j
public class ChangelogDialog extends JDialog {

    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/lakernote/easy-postman/releases";
    private static final String GITEE_RELEASES_URL = "https://gitee.com/api/v5/repos/lakernote/easy-postman/releases";
    private static final String GITHUB_WEB_URL = "https://github.com/lakernote/easy-postman/releases";
    private static final String GITEE_WEB_URL = "https://gitee.com/lakernote/easy-postman/releases";

    private final JTextArea contentArea;
    private final JButton refreshButton;
    private final JLabel statusLabel;

    public ChangelogDialog(Frame parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.CHANGELOG_TITLE), true);

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
        githubButton.addActionListener(e -> openUrl(GITHUB_WEB_URL));

        JButton giteeButton = new JButton(I18nUtil.getMessage(MessageKeys.CHANGELOG_VIEW_ON_GITEE));
        giteeButton.addActionListener(e -> openUrl(GITEE_WEB_URL));

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

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        String currentVersion = SystemUtil.getCurrentVersion();
        JLabel versionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CHANGELOG_CURRENT_VERSION, currentVersion));
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 14));

        panel.add(versionLabel, BorderLayout.WEST);

        return panel;
    }


    private void loadChangelog() {
        contentArea.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));
        refreshButton.setEnabled(false);
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.CHANGELOG_LOADING));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                // 根据用户设置的更新源偏好选择获取源
                String preference = SettingManager.getUpdateSourcePreference();

                String result = null;
                if ("github".equals(preference)) {
                    // 用户指定使用 GitHub
                    log.info("Using user-preferred source: GitHub");
                    result = fetchFromGitHub();
                    if (result == null) {
                        log.info("GitHub API failed, trying Gitee as fallback");
                        result = fetchFromGitee();
                    }
                } else if ("gitee".equals(preference)) {
                    // 用户指定使用 Gitee
                    log.info("Using user-preferred source: Gitee");
                    result = fetchFromGitee();
                    if (result == null) {
                        log.info("Gitee API failed, trying GitHub as fallback");
                        result = fetchFromGitHub();
                    }
                } else {
                    // auto 模式：优先尝试 Gitee（国内用户居多）
                    log.info("Auto mode: trying Gitee first");
                    result = fetchFromGitee();
                    if (result == null) {
                        log.info("Gitee API failed, trying GitHub API");
                        result = fetchFromGitHub();
                    }
                }
                return result;
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

    private String fetchFromGitee() {
        try {
            log.info("Fetching changelog from Gitee API");
            HttpResponse response = HttpRequest.get(GITEE_RELEASES_URL)
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                String body = response.body();
                return parseGiteeReleases(body);
            } else {
                log.warn("Gitee API returned status: {}", response.getStatus());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch from Gitee: {}", e.getMessage());
        }
        return null;
    }

    private String fetchFromGitHub() {
        try {
            log.info("Fetching changelog from GitHub API");
            HttpResponse response = HttpRequest.get(GITHUB_RELEASES_URL)
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                String body = response.body();
                return parseGitHubReleases(body);
            } else {
                log.warn("GitHub API returned status: {}", response.getStatus());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch from GitHub: {}", e.getMessage());
        }
        return null;
    }

    private String parseGiteeReleases(String jsonBody) {
        try {
            JSONArray releases = JSONUtil.parseArray(jsonBody);
            if (releases.isEmpty()) {
                return I18nUtil.getMessage(MessageKeys.CHANGELOG_NO_RELEASES);
            }

            // 按创建时间降序排序，确保最新版本在最前面
            List<JSONObject> releaseList = new ArrayList<>();
            for (int i = 0; i < releases.size(); i++) {
                releaseList.add(releases.getJSONObject(i));
            }
            releaseList.sort((a, b) -> {
                String dateA = a.getStr("created_at", "");
                String dateB = b.getStr("created_at", "");
                return dateB.compareTo(dateA); // 降序：最新的在前
            });

            StringBuilder sb = new StringBuilder();
            // 最多显示最近10个版本
            for (int i = 0; i < Math.min(releaseList.size(), 10); i++) {
                JSONObject release = releaseList.get(i);
                String tagName = release.getStr("tag_name");
                String name = release.getStr("name");
                String body = release.getStr("body");
                String createdAt = release.getStr("created_at");

                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("📦 ").append(StrUtil.isNotBlank(name) ? name : tagName).append("\n");
                sb.append("📅 ").append(createdAt != null && createdAt.length() >= 10 ? createdAt.substring(0, 10) : "Unknown").append("\n");
                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

                if (StrUtil.isNotBlank(body)) {
                    sb.append(body.trim()).append("\n\n\n");
                } else {
                    sb.append("无更新说明\n\n\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to parse Gitee releases", e);
            return null;
        }
    }

    private String parseGitHubReleases(String jsonBody) {
        try {
            JSONArray releases = JSONUtil.parseArray(jsonBody);
            if (releases.isEmpty()) {
                return I18nUtil.getMessage(MessageKeys.CHANGELOG_NO_RELEASES);
            }

            // 按发布时间降序排序，确保最新版本在最前面
            List<JSONObject> releaseList = new ArrayList<>();
            for (int i = 0; i < releases.size(); i++) {
                releaseList.add(releases.getJSONObject(i));
            }
            releaseList.sort((a, b) -> {
                String dateA = a.getStr("published_at", "");
                String dateB = b.getStr("published_at", "");
                return dateB.compareTo(dateA); // 降序：最新的在前
            });

            StringBuilder sb = new StringBuilder();
            // 最多显示最近10个版本
            for (int i = 0; i < Math.min(releaseList.size(), 10); i++) {
                JSONObject release = releaseList.get(i);
                String tagName = release.getStr("tag_name");
                String name = release.getStr("name");
                String body = release.getStr("body");
                String publishedAt = release.getStr("published_at");

                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("📦 ").append(StrUtil.isNotBlank(name) ? name : tagName).append("\n");
                sb.append("📅 ").append(publishedAt != null && publishedAt.length() >= 10 ? publishedAt.substring(0, 10) : "Unknown").append("\n");
                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

                if (StrUtil.isNotBlank(body)) {
                    sb.append(body.trim()).append("\n\n\n");
                } else {
                    sb.append("No release notes provided\n\n\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to parse GitHub releases", e);
            return null;
        }
    }

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

